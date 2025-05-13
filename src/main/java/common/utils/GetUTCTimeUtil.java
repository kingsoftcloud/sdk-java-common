package common.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Classname GetUTCTimeUtil
 * @Description 获取utc时间
 */
public class GetUTCTimeUtil {


    private static ThreadLocal<SimpleDateFormat> utcFormatThreadLocal = ThreadLocal.withInitial(() -> {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return dateFormat;
    });

    public static String getUTCTimeStr() {
        // 1、取得本地时间，设置时区
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        // 2、取得时间偏移量：
        int zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET);
        // 3、取得夏令时差：
        int dstOffset = cal.get(java.util.Calendar.DST_OFFSET);
        // 4、从本地时间里扣除这些差量，即可以取得UTC时间：
        cal.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        Date utcDate = cal.getTime();
        return utcFormatThreadLocal.get().format(utcDate);
    }


    public static void main(String[] args) {
        int corePoolSize = 5; // 核心线程数
        int maxPoolSize = 10; // 最大线程数
        long keepAliveTime = 60L; // 线程存活时间
        TimeUnit unit = TimeUnit.SECONDS;
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100); // 任务队列，可以根据需求选择不同的队列类型

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                corePoolSize, // 核心线程数
                maxPoolSize, // 最大线程数
                keepAliveTime, // 线程存活时间
                unit, // 存活时间单位
                workQueue // 任务队列
        );

        for (int i=0;i<10000;i++){
            // 使用线程池执行任务
            threadPoolExecutor.execute(() -> {
                String utcTimeStr = getUTCTimeStr();
                System.out.println("utcTimeStr="+utcTimeStr);
            });
        }

        // 关闭线程池
        threadPoolExecutor.shutdown();
    }


}
