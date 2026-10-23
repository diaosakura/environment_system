package edu.net.environment.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import edu.net.environment.model.ApiException;
import edu.net.environment.model.Requests.Measurement;
import org.springframework.stereotype.Component;

@Component
public class AqiCalculator {
    public record Result(int so2Level,int coLevel,int pm25Level,int grade) {}
    public int level(BigDecimal value,List<Map<String,Object>> levels,String key) {
        ApiException.require(value != null && value.signum() >= 0, "浓度必须为非负数");
        for (var row : levels) {
            Object max = row.get(key);
            if (max == null || value.compareTo(new BigDecimal(max.toString())) <= 0)
                return ((Number) row.get("id")).intValue();
        }
        throw new ApiException(500,"空气等级字典尚未初始化");
    }
    public Result calculate(Measurement m,List<Map<String,Object>> levels) {
        int a=level(m.so2(),levels,"so2Max"), b=level(m.co(),levels,"coMax"), c=level(m.pm25(),levels,"pm25Max");
        return new Result(a,b,c,Math.max(a,Math.max(b,c)));
    }
}
