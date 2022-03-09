package org.songjiyang;

import org.songjiyang.enums.MetricKey;
import org.songjiyang.enums.StatKey;
import org.songjiyang.service.StatisticService;
import org.songjiyang.vo.ConfigStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistic")
public class StatisticController {


    @Autowired
    private StatisticService statisticService;

    @RequestMapping(value = "/collect", method = RequestMethod.POST)
    public String search(@RequestBody Map<String, Integer> mertricToCount) {

        statisticService.collect(StatKey.FRONTEND.name(), mertricToCount);
        return "success";
    }

    @RequestMapping(value = "/config_stat", method = RequestMethod.GET)
    public List<ConfigStat> configStat() {

        return statisticService.configStat();
    }

    @RequestMapping(value = "/api_one", method = RequestMethod.GET)
    public String apiOne() {

        Map<String, Integer> mertricToCount = new HashMap<>();
        mertricToCount.put(MetricKey.API_ONE.name(), 1);
        statisticService.collect(StatKey.API.name(), mertricToCount);

        return "hello world";
    }

    @RequestMapping(value = "/last_month_api_user", method = RequestMethod.GET)
    public Map<String, Integer> lastMonthApiUse() {


        return statisticService.lastMonthUseCount(StatKey.API.name());
    }
}
