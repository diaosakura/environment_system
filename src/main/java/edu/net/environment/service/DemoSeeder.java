package edu.net.environment.service;

import edu.net.environment.mapper.EnvironmentMapper;
import edu.net.environment.model.Requests.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoSeeder {
    private final EnvironmentMapper mapper;
    private final AqiCalculator calculator;
    public DemoSeeder(EnvironmentMapper mapper,AqiCalculator calculator) { this.mapper=mapper; this.calculator=calculator; }
    @Transactional
    public void seed() {
        if (mapper.adminCount()>0) throw new IllegalStateException("Database already initialized. Existing data was not changed.");
        var encoder=new BCryptPasswordEncoder();
        mapper.seedAdmin(encoder.encode("Admin123456"));
        String[] provinceNames={"北京市","天津市","河北省","山西省","内蒙古自治区","辽宁省","吉林省","黑龙江省","上海市","江苏省","浙江省","安徽省","福建省","江西省","山东省","河南省"};
        String[] abbreviations={"京","津","冀","晋","蒙","辽","吉","黑","沪","苏","浙","皖","闽","赣","鲁","豫"};
        String[] cities={"北京市","天津市","石家庄市","太原市","呼和浩特市","沈阳市","长春市","哈尔滨市","上海市","南京市","杭州市","合肥市","福州市","南昌市","济南市","郑州市"};
        for (int i=0;i<16;i++) {
            mapper.seedProvince(i+1,provinceNames[i],abbreviations[i]); mapper.seedCity(i+1,cities[i],i+1);
        }
        mapper.seedCity(17,"大连市",6);
        String[] names={"优","良","轻度污染","中度污染","重度污染","严重污染"};
        String[] chinese={"一","二","三","四","五","六"};
        String[] colors={"#22c55e","#eab308","#f97316","#ef4444","#a855f7","#881337"};
        String[] descriptions={"空气质量令人满意","空气质量可接受","敏感人群宜减少户外活动","敏感人群应减少户外活动","敏感人群应避免户外活动","建议减少户外活动"};
        Integer[] so2Max={50,150,475,800,1600,null},coMax={5,10,35,60,90,null},pmMax={35,75,115,150,250,null};
        for (int i=0;i<6;i++) {
            var row=new HashMap<String,Object>();
            row.put("id",i+1); row.put("chinese",chinese[i]); row.put("name",names[i]); row.put("color",colors[i]);
            row.put("description",descriptions[i]); row.put("so2",so2Max[i]); row.put("co",coMax[i]); row.put("pm25",pmMax[i]);
            mapper.seedLevel(row);
        }
        mapper.register(new Register("13800000001","演示监督员甲",LocalDate.of(2002,5,8),1,"User123456"),encoder.encode("User123456"));
        mapper.register(new Register("13800000002","演示监督员乙",LocalDate.of(2001,8,12),0,"User123456"),encoder.encode("User123456"));
        int[] gridCities={6,17,11,10},gridProvinces={6,6,11,10};
        String[] gridNames={"沈阳网格员","大连网格员","杭州网格员","南京网格员"};
        var gridIds=new ArrayList<Long>();
        for (int i=0;i<4;i++) {
            var output=new HashMap<String,Object>();
            mapper.createGrid(new GridMember("grid"+(i+1),gridNames[i],"1390000000"+(i+1),gridProvinces[i],gridCities[i],true),
                encoder.encode("Grid123456"),output);
            gridIds.add(((Number)output.get("id")).longValue());
        }
        var levels=mapper.levels();
        int[] so2={25,80,200,600,1000,1800},co={2,8,20,45,75,100},pm={20,55,90,130,190,300};
        for (int i=0;i<40;i++) {
            int g=i%4,grade=i%6+1,state=i<12?0:i<20?1:2;
            LocalDateTime submitted;
            if (state==2) {
                var month=YearMonth.now().minusMonths((i-20)%12);
                submitted=month.atDay(Math.min(10,LocalDate.now().getDayOfMonth())).atTime(9,0).plusMinutes(i);
            } else submitted=LocalDateTime.now().minusDays(40-i).withNano(0);
            var row=new HashMap<String,Object>();
            row.put("phone",i%2==0?"13800000001":"13800000002"); row.put("provinceId",gridProvinces[g]); row.put("cityId",gridCities[g]);
            row.put("address","演示街道"+(i+1)+"号（虚构地址）"); row.put("information","课程演示反馈：发现扬尘或异味，请核查现场空气质量。");
            row.put("grade",grade); row.put("state",state); row.put("gridId",state==0?null:gridIds.get(g));
            row.put("submitted",submitted); row.put("assigned",state==0?null:submitted.plusMinutes(10));
            row.put("confirmed",state==2?submitted.plusHours(2):null); mapper.seedFeedback(row);
            if (state==2) {
                var m=new Measurement(BigDecimal.valueOf(so2[grade-1]),BigDecimal.valueOf(co[grade-1]),BigDecimal.valueOf(pm[grade-1]));
                var result=calculator.calculate(m,levels);
                mapper.measure(((Number)row.get("id")).longValue(),m,result.so2Level(),result.coLevel(),result.pm25Level(),result.grade(),submitted.plusHours(2));
            }
        }
        mapper.log("system","初始化脚本","system","初始化演示数据","40");
    }
}
