package edu.net.environment.service;

import edu.net.environment.model.ApiException;
import edu.net.environment.model.Requests.Measurement;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class AqiCalculatorTest {
    private final AqiCalculator calculator=new AqiCalculator();
    static List<Map<String,Object>> levels() {
        Integer[] so2={50,150,475,800,1600,null},co={5,10,35,60,90,null},pm={35,75,115,150,250,null};
        var result=new ArrayList<Map<String,Object>>();
        for (int i=0;i<6;i++) {
            var row=new HashMap<String,Object>();
            row.put("id",i+1); row.put("so2Max",so2[i]); row.put("coMax",co[i]); row.put("pm25Max",pm[i]); result.add(row);
        }
        return result;
    }
    @ParameterizedTest
    @CsvSource({"0,so2Max,1","50,so2Max,1","50.001,so2Max,2","150,so2Max,2","150.001,so2Max,3",
        "5,coMax,1","5.001,coMax,2","10,coMax,2","10.001,coMax,3","35,pm25Max,1","35.001,pm25Max,2",
        "75,pm25Max,2","75.001,pm25Max,3","250,pm25Max,5","250.001,pm25Max,6","9999999,pm25Max,6"})
    void continuousBoundaries(String value,String key,int expected) {
        assertEquals(expected,calculator.level(new BigDecimal(value),levels(),key));
    }
    @Test void finalGradeUsesWorstPollutant() {
        var result=calculator.calculate(new Measurement(new BigDecimal("20"),new BigDecimal("45"),new BigDecimal("90")),levels());
        assertEquals(1,result.so2Level()); assertEquals(4,result.coLevel()); assertEquals(3,result.pm25Level()); assertEquals(4,result.grade());
    }
    @Test void negativeAndMissingConcentrationsAreRejected() {
        assertThrows(ApiException.class,()->calculator.level(new BigDecimal("-0.001"),levels(),"so2Max"));
        assertThrows(ApiException.class,()->calculator.level(null,levels(),"coMax"));
    }
}
