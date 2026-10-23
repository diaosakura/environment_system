package edu.net.environment.service;

import edu.net.environment.config.Audited;
import edu.net.environment.mapper.EnvironmentMapper;
import edu.net.environment.model.*;
import edu.net.environment.model.Requests.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentService {
    private final EnvironmentMapper mapper;
    private final AqiCalculator calculator;
    private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder();
    public EnvironmentService(EnvironmentMapper mapper,AqiCalculator calculator) { this.mapper=mapper; this.calculator=calculator; }
    public static boolean enabled(Object value) { return Boolean.TRUE.equals(value) || (value instanceof Number n && n.intValue()==1); }
    private static long number(Map<String,Object> row,String key) { return ((Number) row.get(key)).longValue(); }
    private static Map<String,Object> exists(Map<String,Object> row) {
        if (row == null) throw new ApiException(404,"记录不存在");
        return row;
    }
    private String hash(String password) {
        ApiException.require(password.getBytes(StandardCharsets.UTF_8).length<=72,"密码的UTF-8长度不能超过72字节");
        return passwords.encode(password);
    }
    public SessionUser login(Login r) {
        ApiException.require(Set.of("admin","grid","supervisor").contains(r.role()),"用户类型不正确");
        var row=mapper.account(r.role(),r.account());
        if (row==null || r.password().getBytes(StandardCharsets.UTF_8).length>72
            || !passwords.matches(r.password(),row.get("passwordHash").toString()))
            throw new ApiException(401,"账号或密码不正确");
        if (!enabled(row.get("enabled"))) throw new ApiException(403,"账号已停用，请联系管理员");
        return new SessionUser(row.get("id").toString(),row.get("name").toString(),r.role());
    }
    @Transactional @Audited("监督员注册")
    public SessionUser register(Register r) {
        mapper.register(r,hash(r.password()));
        return new SessionUser(r.phone(),r.name(),"supervisor");
    }
    public List<Map<String,Object>> provinces() { return mapper.provinces(); }
    public List<Map<String,Object>> cities(int id) { return mapper.cities(id); }
    public List<Map<String,Object>> levels() { return mapper.levels(); }
    private void city(int provinceId,int cityId) { ApiException.require(mapper.validCity(provinceId,cityId)==1,"请选择正确的省市"); }
    @Transactional @Audited("提交公众反馈")
    public Map<String,Object> createFeedback(SessionUser user,Feedback r) {
        city(r.provinceId(),r.cityId());
        var output=new HashMap<String,Object>();
        mapper.createFeedback(user.id(),r,output);
        return output;
    }
    public record Page<T>(List<T> records,long total,int page,int size) {}
    public static int positive(String value,int fallback,int max) {
        if (value==null || value.isBlank()) return fallback;
        try { int n=Integer.parseInt(value); ApiException.require(n>0 && n<=max,"分页或编号超出范围"); return n; }
        catch(NumberFormatException ex) { throw new ApiException(400,"数字参数不正确"); }
    }
    private static Integer integer(String value) { return value==null || value.isBlank() ? null : positive(value,1,Integer.MAX_VALUE); }
    public Page<Map<String,Object>> feedbacks(SessionUser user,Map<String,String> params) {
        var filter=new HashMap<String,Object>();
        int page=positive(params.get("page"),1,1000000), size=positive(params.get("size"),10,100);
        filter.put("size",size); filter.put("offset",(page-1)*size);
        if (user.role().equals("supervisor")) filter.put("owner",user.id());
        if (user.role().equals("grid")) filter.put("gridId",user.id());
        if (params.get("state")!=null && !params.get("state").isBlank()) {
            String state=params.get("state");
            ApiException.require(Set.of("0","1","2").contains(state),"反馈状态不正确");
            filter.put("state",Integer.valueOf(state));
        }
        filter.put("provinceId",integer(params.get("provinceId")));
        filter.put("cityId",integer(params.get("cityId")));
        String keyword=params.get("keyword");
        if (keyword!=null && !keyword.isBlank()) filter.put("keyword",keyword.trim());
        try {
            LocalDate start=params.getOrDefault("start","").isBlank()?null:LocalDate.parse(params.get("start"));
            LocalDate end=params.getOrDefault("end","").isBlank()?null:LocalDate.parse(params.get("end"));
            ApiException.require(start==null || end==null || !end.isBefore(start),"结束日期不能早于开始日期");
            filter.put("start",start); filter.put("end",end==null?null:end.plusDays(1));
        } catch(DateTimeParseException ex) { throw new ApiException(400,"日期格式应为YYYY-MM-DD"); }
        return new Page<>(mapper.feedbacks(filter),mapper.feedbackCount(filter),page,size);
    }
    public Map<String,Object> detail(SessionUser user,long id) {
        var row=exists(mapper.feedback(id));
        if ((user.role().equals("supervisor") && !user.id().equals(row.get("supervisorId")))
            || (user.role().equals("grid") && !user.id().equals(String.valueOf(row.get("gridMemberId")))))
            throw new ApiException(403,"只能查看自己的记录");
        if (user.role().equals("supervisor")) row.remove("supervisorId");
        return row;
    }
    @Transactional @Audited("指派网格员")
    public void assign(SessionUser user,long id,Assignment r) {
        var row=exists(mapper.lockFeedback(id));
        if (number(row,"state")!=0) throw new ApiException(409,"此反馈已被指派，请刷新列表");
        var grid=exists(mapper.lockGrid(r.gridMemberId()));
        ApiException.require(enabled(grid.get("enabled")),"网格员已停用");
        ApiException.require(number(grid,"cityId")==number(row,"cityId"),"只能指派所在城市的网格员");
        if (mapper.assign(id,r.gridMemberId())!=1) throw new ApiException(409,"反馈状态已变化");
    }
    @Transactional @Audited("提交实测结果")
    public Map<String,Object> measure(SessionUser user,long id,Measurement r) {
        var row=exists(mapper.lockFeedback(id));
        if (!user.id().equals(String.valueOf(row.get("gridMemberId")))) throw new ApiException(403,"只能处理指派给自己的任务");
        if (number(row,"state")!=1) throw new ApiException(409,"此任务已经完成，请勿重复提交");
        var result=calculator.calculate(r,mapper.levels());
        var time=LocalDateTime.now();
        mapper.measure(id,r,result.so2Level(),result.coLevel(),result.pm25Level(),result.grade(),time);
        if (mapper.complete(id,time)!=1) throw new ApiException(409,"任务状态已变化");
        return Map.of("id",id,"grade",result.grade());
    }
    public List<Map<String,Object>> gridMembers(Integer cityId,String keyword) { return mapper.gridMembers(cityId,keyword); }
    @Transactional @Audited("新增网格员")
    public Map<String,Object> createGrid(SessionUser user,GridMember r) {
        city(r.provinceId(),r.cityId());
        var output=new HashMap<String,Object>();
        mapper.createGrid(r,hash("Grid123456"),output);
        return output;
    }
    @Transactional @Audited("修改网格员")
    public void updateGrid(SessionUser user,long id,GridMember r) {
        var current=exists(mapper.lockGrid(id));
        city(r.provinceId(),r.cityId());
        if ((!r.enabled() || number(current,"cityId")!=r.cityId()) && mapper.pendingTasks(id)>0)
            throw new ApiException(409,"网格员有待实测任务，暂不能停用或变更城市");
        if (mapper.updateGrid(id,r)!=1) throw new ApiException(404,"网格员不存在");
    }
    public List<Map<String,Object>> supervisors(String keyword) { return mapper.supervisors(keyword); }
    @Transactional @Audited("修改监督员")
    public void updateSupervisor(SessionUser user,String id,Supervisor r) {
        if (mapper.updateSupervisor(id,r)!=1) throw new ApiException(404,"监督员不存在");
    }
    @Transactional @Audited("重置网格员密码")
    public void resetGridPassword(SessionUser user,String id,Password r) { reset("grid",id,r); }
    @Transactional @Audited("重置监督员密码")
    public void resetSupervisorPassword(SessionUser user,String id,Password r) { reset("supervisor",id,r); }
    private void reset(String role,String id,Password r) {
        if (mapper.resetPassword(role,id,hash(r.password()))!=1) throw new ApiException(404,"用户不存在");
    }
    public Page<Map<String,Object>> logs(int page,int size) {
        return new Page<>(mapper.logs(size,(page-1)*size),mapper.logCount(),page,size);
    }
    @Transactional(readOnly=true)
    public Map<String,Object> statistics() {
        var first=YearMonth.now().minusMonths(11);
        var raw=mapper.trend(first.atDay(1).atStartOfDay());
        var indexed=new HashMap<String,Map<String,Object>>();
        raw.forEach(row -> indexed.put(row.get("month").toString(),row));
        var trend=new ArrayList<Map<String,Object>>();
        for (int i=0;i<12;i++) {
            String month=first.plusMonths(i).toString();
            trend.add(indexed.getOrDefault(month,Map.of("month",month,"total",0,"polluted",0)));
        }
        return Map.of("summary",mapper.summary(),"distribution",mapper.distribution(),"trend",trend,
            "regions",mapper.regions(),"updatedAt",LocalDateTime.now().toString());
    }
    public int ping() { return mapper.ping(); }
}
