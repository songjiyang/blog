package org.songjiyang.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.songjiyang.dao.StatisticDAO;
import org.songjiyang.enums.MetricKey;
import org.songjiyang.enums.StatKey;
import org.songjiyang.vo.ConfigStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticService.class);

    @Autowired
    private StatisticDAO statisticDAO;

    public void collect(String key, Map<String, Integer> metricToCount) {

        statisticDAO.collectDaily(key, metricToCount);
    }

    @PostConstruct
    public void collectEveryConfigNum() {

        Map<String, Integer> metricToCount = new HashMap<>();

        metricToCount.put(MetricKey.CPU_NUM.name(), Runtime.getRuntime().availableProcessors());
        metricToCount.put(MetricKey.FREE_MEM.name(), (int) Runtime.getRuntime().freeMemory());
        metricToCount.put(MetricKey.MAX_MEM.name(), (int) Runtime.getRuntime().maxMemory());
        metricToCount.put(MetricKey.JVM_MEM.name(), (int) Runtime.getRuntime().totalMemory());

        statisticDAO.collect(StatKey.SYSTEM_INFO.name(), metricToCount);
    }

    public Map<String, Integer> lastMonthUseCount(String key) {

        try {
            Map<String, Integer> rs = new HashMap<>();

            LocalDate now = LocalDate.now();
            LocalDate lastMonthDate = now.minusDays(29);
            LocalDate endDate = now.plusDays(1);

            Map<String, Map<String, Integer>> dateToUseCount = statisticDAO.queryTimeRange(key, lastMonthDate, endDate);

            for (Map<String, Integer> metricToCount : dateToUseCount.values()) {
                for (Map.Entry<String, Integer> entry : metricToCount.entrySet()) {
                    rs.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }

            }
            return rs;
        } catch (Exception e) {
            LOGGER.error("StatisticManager lastMonthUseCount error", e);
            return new HashMap<>();
        }
    }

    public List<ConfigStat> configStat() {
        List<ConfigStat> rs = new ArrayList<>();
        Map<String, Integer> typeToTotalNum = statisticDAO.query(StatKey.SYSTEM_INFO.name());
        for (String type : typeToTotalNum.keySet()) {
            ConfigStat configStat = new ConfigStat();

            configStat.setType(type);
            configStat.setNum(typeToTotalNum.get(type));
            rs.add(configStat);
        }
        return rs;
    }

}
