package org.songjiyang.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class StatisticDAO {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	private static final String STATISTIC_PREFIX = "STATISTIC:";

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd" );

	/**
	 * 覆盖收集数据，永久保存
	 *
	 * @param key           数据分类（类似MySQL表）
	 * @param metricToCount 指标-数量
	 */
	public void collect( String key, Map<String, Integer> metricToCount ){

		key = makeKey( key );
		String finalKey = key;
		metricToCount.forEach( ( oneMetric, value ) -> {
			redisTemplate.opsForZSet().add( finalKey, oneMetric, value );
		} );
	}

	/**
	 * 按天增量收集数据，保存30天
	 *
	 * @param key           数据分类（类似MySQL表）
	 * @param metricToCount 指标-数量
	 */
	public void collectDaily( String key, Map<String, Integer> metricToCount ){

		key = makeDailyKey( key );
		String finalKey = key;

		metricToCount.forEach( ( oneMetric, value ) -> {
			redisTemplate.opsForZSet().incrementScore( finalKey, oneMetric, value );
		} );

		Long expire = redisTemplate.getExpire( finalKey );

		if( expire != null && expire == -1 ){
			redisTemplate.expire( finalKey, 30, TimeUnit.DAYS );
		}
	}

	private Map<String, Integer> queryDirectly( String key ){

		Map<String, Integer> rs = new HashMap<>();

		Set<ZSetOperations.TypedTuple<String>> mertricToCountTuple = redisTemplate.opsForZSet().rangeWithScores( key, 0, -1 );

		if( mertricToCountTuple != null ){
			for( ZSetOperations.TypedTuple<String> oneMetricCount : mertricToCountTuple ){
				if( oneMetricCount.getScore() != null ){
					rs.put( oneMetricCount.getValue(), oneMetricCount.getScore().intValue() );
				}
			}
		}

		return rs;
	}

	/**
	 * 根据数据分类查询数据
	 *
	 * @param key 数据分类
	 * @return 指标-数量
	 */
	public Map<String, Integer> query( String key ){

		key = this.makeKey( key );
		return queryDirectly( key );
	}

	/**
	 * 根据数据分类和指定时间段查询数据
	 *
	 * @param key   数据分类
	 * @param start 开始时间
	 * @param end   结束时间
	 * @return 指标-数量
	 */
	public Map<String, Map<String, Integer>> queryTimeRange( String key, LocalDate start, LocalDate end ){

		Map<String, Map<String, Integer>> rs = new HashMap<>();

		List<LocalDate> keys = new ArrayList<>();

		List<Object> tupleSets = redisTemplate.executePipelined( ( RedisCallback<Object> )redisConnection -> {

			redisConnection.openPipeline();
			LocalDate dayInRange = start;
			for( ; dayInRange.isBefore( end ); dayInRange = dayInRange.plusDays( 1 ) ){
				String dayKey = makeDailyKey( key, dayInRange );
				keys.add( dayInRange );
				redisConnection.zRangeWithScores( dayKey.getBytes( StandardCharsets.UTF_8 ), 0, -1 );

			}
			return null;
		} );

		for( int i = 0; i < keys.size(); i++ ){
			@SuppressWarnings( "unchecked" )
			Set<DefaultTypedTuple<String>> tupleSet = ( Set<DefaultTypedTuple<String>> )tupleSets.get( i );
			Map<String, Integer> metricToCount = new HashMap<>();

			for( DefaultTypedTuple<String> tuple : tupleSet ){
				if( tuple.getScore() != null ){
					metricToCount.put( tuple.getValue(), tuple.getScore().intValue() );

				}
			}
			rs.put( keys.get( i ).toString(), metricToCount );
		}
		return rs;

	}

	private String makeKey( String key ){

		return STATISTIC_PREFIX + key;
	}

	private String makeDailyKey( String key ){

		return STATISTIC_PREFIX + LocalDate.now() + ":" + key;
	}

	private String makeDailyKey( String key, LocalDate date ){

		return STATISTIC_PREFIX + date + ":" + key;
	}

}
