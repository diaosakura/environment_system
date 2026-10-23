package edu.net.environment.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;
import edu.net.environment.model.Requests.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EnvironmentMapper {
    @Select("SELECT 1") int ping();
    @Select("""
        <script>
        <choose>
        <when test="role == 'admin'">SELECT CAST(admin_id AS CHAR) id, real_name name, password_hash passwordHash, 1 enabled FROM admins WHERE admin_code=#{account}</when>
        <when test="role == 'grid'">SELECT CAST(gm_id AS CHAR) id, gm_name name, password_hash passwordHash, enabled FROM grid_member WHERE gm_code=#{account}</when>
        <otherwise>SELECT tel_id id, real_name name, password_hash passwordHash, enabled FROM supervisor WHERE tel_id=#{account}</otherwise>
        </choose>
        </script>
        """)
    Map<String,Object> account(@Param("role") String role,@Param("account") String account);
    @Select("""
        <script><choose>
        <when test="role == 'grid'">SELECT COUNT(*) FROM grid_member WHERE gm_id=#{id} AND enabled=1</when>
        <otherwise>SELECT COUNT(*) FROM supervisor WHERE tel_id=#{id} AND enabled=1</otherwise>
        </choose></script>
        """)
    int activeAccount(@Param("role") String role,@Param("id") String id);
    @Insert("INSERT INTO supervisor(tel_id,password_hash,real_name,birthday,sex) VALUES(#{r.phone},#{hash},#{r.name},#{r.birthday},#{r.sex})")
    int register(@Param("r") Register r,@Param("hash") String hash);
    @Select("SELECT province_id id,province_name name,province_abbr abbr FROM grid_province ORDER BY province_id")
    List<Map<String,Object>> provinces();
    @Select("SELECT city_id id,city_name name,province_id provinceId FROM grid_city WHERE province_id=#{provinceId} ORDER BY city_id")
    List<Map<String,Object>> cities(int provinceId);
    @Select("SELECT aqi_id id,chinese_explain chinese,aqi_explain name,color,health_impact description,so2_max so2Max,co_max coMax,spm_max pm25Max FROM aqi ORDER BY aqi_id")
    List<Map<String,Object>> levels();
    @Select("SELECT COUNT(*) FROM grid_city WHERE province_id=#{provinceId} AND city_id=#{cityId}")
    int validCity(@Param("provinceId") int provinceId,@Param("cityId") int cityId);
    @Insert("""
        INSERT INTO aqi_feedback(tel_id,province_id,city_id,address,information,estimated_grade)
        VALUES(#{userId},#{r.provinceId},#{r.cityId},#{r.address},#{r.information},#{r.estimatedGrade})
        """)
    @Options(useGeneratedKeys=true,keyProperty="output.id")
    int createFeedback(@Param("userId") String userId,@Param("r") Feedback r,@Param("output") Map<String,Object> output);

    // Multi-table queries and aggregate statistics are defined in mapper/EnvironmentMapper.xml.
    long feedbackCount(Map<String,Object> filter);
    List<Map<String,Object>> feedbacks(Map<String,Object> filter);
    Map<String,Object> feedback(long id);
    @Select("SELECT af_id id,tel_id supervisorId,gm_id gridMemberId,city_id cityId,state FROM aqi_feedback WHERE af_id=#{id} FOR UPDATE")
    Map<String,Object> lockFeedback(long id);
    @Select("SELECT gm_id id,city_id cityId,enabled FROM grid_member WHERE gm_id=#{id} FOR UPDATE")
    Map<String,Object> lockGrid(long id);
    @Update("UPDATE aqi_feedback SET gm_id=#{gridId},state=1,assigned_at=CURRENT_TIMESTAMP WHERE af_id=#{id} AND state=0")
    int assign(@Param("id") long id,@Param("gridId") long gridId);
    @Insert("""
        INSERT INTO statistics(feedback_id,so2_value,co_value,spm_value,so2_level,co_level,spm_level,aqi_id,confirmed_at)
        VALUES(#{id},#{m.so2},#{m.co},#{m.pm25},#{so2Level},#{coLevel},#{pm25Level},#{grade},#{time})
        """)
    int measure(@Param("id") long id,@Param("m") Measurement m,@Param("so2Level") int so2Level,
        @Param("coLevel") int coLevel,@Param("pm25Level") int pm25Level,@Param("grade") int grade,@Param("time") LocalDateTime time);
    @Update("UPDATE aqi_feedback SET state=2,confirmed_at=#{time} WHERE af_id=#{id} AND state=1")
    int complete(@Param("id") long id,@Param("time") LocalDateTime time);
    @Select("""
        <script>SELECT g.gm_id id,g.gm_code account,g.gm_name name,g.tel phone,g.province_id provinceId,
        p.province_name provinceName,g.city_id cityId,c.city_name cityName,g.enabled,
        (SELECT COUNT(*) FROM aqi_feedback f WHERE f.gm_id=g.gm_id AND f.state=1) pendingCount
        FROM grid_member g JOIN grid_province p ON p.province_id=g.province_id JOIN grid_city c ON c.city_id=g.city_id
        <where><if test="cityId != null">AND g.city_id=#{cityId}</if>
        <if test="keyword != null">AND (g.gm_name LIKE CONCAT('%',#{keyword},'%') OR g.gm_code LIKE CONCAT('%',#{keyword},'%'))</if>
        </where> ORDER BY g.gm_id</script>
        """)
    List<Map<String,Object>> gridMembers(@Param("cityId") Integer cityId,@Param("keyword") String keyword);
    @Insert("INSERT INTO grid_member(gm_code,password_hash,gm_name,tel,province_id,city_id,enabled) VALUES(#{r.account},#{hash},#{r.name},#{r.phone},#{r.provinceId},#{r.cityId},#{r.enabled})")
    @Options(useGeneratedKeys=true,keyProperty="output.id")
    int createGrid(@Param("r") GridMember r,@Param("hash") String hash,@Param("output") Map<String,Object> output);
    @Update("UPDATE grid_member SET gm_code=#{r.account},gm_name=#{r.name},tel=#{r.phone},province_id=#{r.provinceId},city_id=#{r.cityId},enabled=#{r.enabled} WHERE gm_id=#{id}")
    int updateGrid(@Param("id") long id,@Param("r") GridMember r);
    @Select("SELECT COUNT(*) FROM aqi_feedback WHERE gm_id=#{id} AND state=1")
    long pendingTasks(long id);
    @Select("""
        <script>SELECT tel_id id,real_name name,birthday,sex,enabled,created_at createdAt FROM supervisor
        <where><if test="keyword != null">AND (real_name LIKE CONCAT('%',#{keyword},'%') OR tel_id LIKE CONCAT('%',#{keyword},'%'))</if></where>
        ORDER BY created_at DESC</script>
        """)
    List<Map<String,Object>> supervisors(String keyword);
    @Update("UPDATE supervisor SET real_name=#{r.name},birthday=#{r.birthday},sex=#{r.sex},enabled=#{r.enabled} WHERE tel_id=#{id}")
    int updateSupervisor(@Param("id") String id,@Param("r") Supervisor r);
    @Update("""
        <script><choose><when test="role == 'grid'">UPDATE grid_member SET password_hash=#{hash} WHERE gm_id=#{id}</when>
        <otherwise>UPDATE supervisor SET password_hash=#{hash} WHERE tel_id=#{id}</otherwise></choose></script>
        """)
    int resetPassword(@Param("role") String role,@Param("id") String id,@Param("hash") String hash);
    @Insert("INSERT INTO operation_log(actor_id,actor_name,actor_role,action,target_id,result) VALUES(#{actorId},#{actorName},#{role},#{action},#{target},'SUCCESS')")
    int log(@Param("actorId") String actorId,@Param("actorName") String actorName,@Param("role") String role,
            @Param("action") String action,@Param("target") String target);
    @Select("SELECT COUNT(*) FROM operation_log") long logCount();
    @Select("SELECT id,actor_id actorId,actor_name actorName,actor_role role,action,target_id targetId,result,created_at createdAt FROM operation_log ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<Map<String,Object>> logs(@Param("size") int size,@Param("offset") int offset);
    Map<String,Object> summary();
    List<Map<String,Object>> distribution();
    List<Map<String,Object>> trend(LocalDateTime since);
    List<Map<String,Object>> regions();

    @Select("SELECT COUNT(*) FROM admins") int adminCount();
    @Insert("INSERT INTO admins(admin_code,password_hash,real_name) VALUES('admin',#{hash},'系统管理员')")
    int seedAdmin(String hash);
    @Insert("INSERT INTO grid_province(province_id,province_name,province_abbr) VALUES(#{id},#{name},#{abbr})")
    int seedProvince(@Param("id") int id,@Param("name") String name,@Param("abbr") String abbr);
    @Insert("INSERT INTO grid_city(city_id,city_name,province_id) VALUES(#{id},#{name},#{provinceId})")
    int seedCity(@Param("id") int id,@Param("name") String name,@Param("provinceId") int provinceId);
    @Insert("INSERT INTO aqi(aqi_id,chinese_explain,aqi_explain,color,health_impact,so2_max,co_max,spm_max) VALUES(#{id},#{chinese},#{name},#{color},#{description},#{so2},#{co},#{pm25})")
    int seedLevel(Map<String,Object> level);
    @Insert("""
        INSERT INTO aqi_feedback(tel_id,province_id,city_id,address,information,estimated_grade,gm_id,state,submitted_at,assigned_at,confirmed_at)
        VALUES(#{phone},#{provinceId},#{cityId},#{address},#{information},#{grade},#{gridId},#{state},#{submitted},#{assigned},#{confirmed})
        """)
    @Options(useGeneratedKeys=true,keyProperty="id")
    int seedFeedback(Map<String,Object> feedback);
}
