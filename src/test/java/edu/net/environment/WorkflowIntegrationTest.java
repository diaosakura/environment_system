package edu.net.environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.net.environment.model.SessionUser;
import edu.net.environment.model.Requests.*;
import edu.net.environment.service.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="RUN_MYSQL_TESTS",matches="true")
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkflowIntegrationTest {
    static final String TEST_DB="environment_system_it_"+Long.toUnsignedString(System.nanoTime(),36);
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",()->"jdbc:mysql://"+System.getenv().getOrDefault("DB_HOST","localhost")+":"
            +System.getenv().getOrDefault("DB_PORT","3306")+"/"+TEST_DB
            +"?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired DemoSeeder seeder;
    @Autowired EnvironmentService service;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @BeforeAll void initializeOwnDatabase() throws Exception {
        var schema=Files.readString(Path.of("sql/schema.sql"),StandardCharsets.UTF_8).replace("environment_system",TEST_DB);
        try (var connection=jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection,new EncodedResource(new ByteArrayResource(schema.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8));
        }
        seeder.seed();
    }
    @AfterAll void removeOnlyOwnTestDatabase() {
        if (!TEST_DB.matches("environment_system_it_[a-z0-9]+")) throw new IllegalStateException("Unsafe test database name");
        jdbc.execute("DROP DATABASE IF EXISTS "+TEST_DB);
    }
    MockHttpSession login(String role,String account,String password) throws Exception {
        var result=mvc.perform(post("/api/auth/login").contentType("application/json")
            .content(json.writeValueAsString(Map.of("role",role,"account",account,"password",password))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value(role)).andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("passwordHash"));
        return (MockHttpSession)result.getRequest().getSession(false);
    }
    long create(MockHttpSession supervisor,String address) throws Exception {
        var response=mvc.perform(post("/api/supervisor/feedbacks").session(supervisor).contentType("application/json")
            .content(json.writeValueAsString(new Feedback(6,6,address,"事务和流程验证",3))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("id").asLong();
    }
    @Test void completeWorkflowAndEnforceOwnership() throws Exception {
        var supervisor=login("supervisor","13800000001","User123456");
        var other=login("supervisor","13800000002","User123456");
        var admin=login("admin","admin","Admin123456");
        var grid=login("grid","grid1","Grid123456");
        var otherGrid=login("grid","grid2","Grid123456");
        long id=create(supervisor,"集成测试地址");
        mvc.perform(get("/api/supervisor/feedbacks/"+id).session(other)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/feedbacks").session(supervisor)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/feedbacks/"+id+"/assignment").session(admin).contentType("application/json").content("{\"gridMemberId\":2}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/feedbacks/"+id+"/assignment").session(admin).contentType("application/json").content("{\"gridMemberId\":1}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/admin/feedbacks/"+id+"/assignment").session(admin).contentType("application/json").content("{\"gridMemberId\":1}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/grid/tasks/"+id+"/measurement").session(otherGrid).contentType("application/json").content("{\"so2\":20,\"co\":8,\"pm25\":90}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/grid/tasks/"+id+"/measurement").session(grid).contentType("application/json").content("{\"so2\":-1,\"co\":8,\"pm25\":90}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/grid/tasks/"+id+"/measurement").session(grid).contentType("application/json").content("{\"so2\":20,\"co\":8,\"pm25\":90}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.grade").value(3));
        mvc.perform(post("/api/grid/tasks/"+id+"/measurement").session(grid).contentType("application/json").content("{\"so2\":20,\"co\":8,\"pm25\":90}"))
            .andExpect(status().isConflict());
        mvc.perform(get("/api/supervisor/feedbacks/"+id).session(supervisor)).andExpect(jsonPath("$.data.state").value(2)).andExpect(jsonPath("$.data.actualGrade").value(3));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM statistics WHERE feedback_id=?",Integer.class,id));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM operation_log WHERE target_id=? AND action IN ('提交公众反馈','指派网格员','提交实测结果')",Integer.class,String.valueOf(id)));
        mvc.perform(get("/api/screen/statistics")).andExpect(status().isOk()).andExpect(jsonPath("$.data.trend.length()").value(12));
        mvc.perform(get("/api/admin/feedbacks")).andExpect(status().isUnauthorized());
    }
    @Test void auditFailureRollsBackFeedbackAndMeasurement() throws Exception {
        var supervisor=login("supervisor","13800000001","User123456");
        long id=create(supervisor,"日志回滚测试");
        service.assign(new SessionUser("1","管理员","admin"),id,new Assignment(1L));
        int before=jdbc.queryForObject("SELECT COUNT(*) FROM aqi_feedback",Integer.class);
        jdbc.execute("CREATE TRIGGER audit_failure BEFORE INSERT ON operation_log FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='intentional audit failure'");
        try {
            mvc.perform(post("/api/supervisor/feedbacks").session(supervisor).contentType("application/json")
                .content("{\"provinceId\":6,\"cityId\":6,\"address\":\"不可提交\",\"information\":\"应回滚\",\"estimatedGrade\":3}"))
                .andExpect(status().isInternalServerError());
            assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM aqi_feedback",Integer.class));
            assertThrows(RuntimeException.class,()->service.measure(new SessionUser("1","网格员","grid"),id,new Measurement(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE)));
            assertEquals(1,jdbc.queryForObject("SELECT state FROM aqi_feedback WHERE af_id=?",Integer.class,id));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM statistics WHERE feedback_id=?",Integer.class,id));
        } finally { jdbc.execute("DROP TRIGGER audit_failure"); }
    }
    @Test void concurrentConfirmationCreatesExactlyOneMeasurement() throws Exception {
        var supervisor=login("supervisor","13800000001","User123456");
        long id=create(supervisor,"并发确认测试");
        service.assign(new SessionUser("1","管理员","admin"),id,new Assignment(1L));
        var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> confirmation=()->{
                try { service.measure(new SessionUser("1","网格员","grid"),id,new Measurement(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE)); return true; }
                catch (edu.net.environment.model.ApiException ex) { assertEquals(409,ex.status()); return false; }
            };
            var results=pool.invokeAll(java.util.List.of(confirmation,confirmation));
            assertNotEquals(results.get(0).get(),results.get(1).get());
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM statistics WHERE feedback_id=?",Integer.class,id));
        } finally { pool.shutdownNow(); }
    }
    @Test void disabledAccountLosesExistingSession() throws Exception {
        var user=login("supervisor","13800000002","User123456");
        jdbc.update("UPDATE supervisor SET enabled=0 WHERE tel_id='13800000002'");
        try { mvc.perform(get("/api/supervisor/feedbacks").session(user)).andExpect(status().isUnauthorized()); }
        finally { jdbc.update("UPDATE supervisor SET enabled=1 WHERE tel_id='13800000002'"); }
    }
    @Test void registrationAndPersonnelMaintenanceWorkEndToEnd() throws Exception {
        String registration=json.writeValueAsString(new Register("13800000003","新监督员",null,1,"Public98765"));
        mvc.perform(post("/api/auth/register").contentType("application/json").content(registration)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/register").contentType("application/json").content(registration)).andExpect(status().isConflict());
        var publicSession=login("supervisor","13800000003","Public98765");
        var admin=login("admin","admin","Admin123456");
        mvc.perform(put("/api/admin/supervisors/13800000003").session(admin).contentType("application/json")
            .content(json.writeValueAsString(new Supervisor("已更新姓名",null,1,false)))).andExpect(status().isOk());
        mvc.perform(get("/api/supervisor/feedbacks").session(publicSession)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/admin/supervisors/13800000003").session(admin).contentType("application/json")
            .content(json.writeValueAsString(new Supervisor("已更新姓名",null,1,true)))).andExpect(status().isOk());
        mvc.perform(put("/api/admin/supervisors/13800000003/password").session(admin).contentType("application/json")
            .content("{\"password\":\"Reset123456\"}")).andExpect(status().isOk());
        login("supervisor","13800000003","Reset123456");
        mvc.perform(get("/api/admin/supervisors").param("keyword","13800000003").session(admin))
            .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].name").value("已更新姓名"));
        var gridBody=new GridMember("grid_test","新网格员","13900000005",6,6,true);
        String created=mvc.perform(post("/api/admin/grid-members").session(admin).contentType("application/json")
            .content(json.writeValueAsString(gridBody))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(created).path("data").path("id").asLong();
        login("grid","grid_test","Grid123456");
        mvc.perform(put("/api/admin/grid-members/"+id).session(admin).contentType("application/json")
            .content(json.writeValueAsString(new GridMember("grid_test","已更新网格员","13900000005",6,17,true))))
            .andExpect(status().isOk());
        mvc.perform(put("/api/admin/grid-members/"+id+"/password").session(admin).contentType("application/json")
            .content("{\"password\":\"GridReset123\"}")).andExpect(status().isOk());
        login("grid","grid_test","GridReset123");
        mvc.perform(get("/api/admin/grid-members").session(admin).param("keyword","grid_test"))
            .andExpect(jsonPath("$.data[0].cityId").value(17));
        mvc.perform(get("/api/admin/operation-logs").session(admin).param("size","100"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.records").isArray());
    }
}
