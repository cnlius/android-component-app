package com.ls.video.utils;

import java.math.BigDecimal;

/**
 * Created by liusong on 2018/1/23.
 */

public class MathUtils {

    /**
     * 除法
     *
     * @param dividends 被除数
     * @param divisor   除数
     * @param scale     精度，四舍五入，到小数点后几位
     * @return dividends/divisor
     */
    public static BigDecimal getDivisionResult(Number dividends, Number divisor, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive number");
        }
        BigDecimal bdDividends = new BigDecimal(String.valueOf(dividends));
        BigDecimal bdDivisor = new BigDecimal(String.valueOf(divisor));
        return bdDividends.divide(bdDivisor, scale, BigDecimal.ROUND_HALF_UP);
    }

    public static float division(float dividends, float divisor, int scale){
        return getDivisionResult(dividends, divisor, scale).floatValue();
    }

    public static double division(double dividends, double divisor, int scale){
        return getDivisionResult(dividends, divisor, scale).doubleValue();
    }

    //--以下未验证----------------------------------------------------------------

    /**
     * 加法
     *
     * @param var1
     * @param var2
     * @return
     */
    public static double add(double var1, double var2) {
        BigDecimal b1 = new BigDecimal(Double.toString(var1));
        BigDecimal b2 = new BigDecimal(Double.toString(var2));
        return b1.add(b2).doubleValue();
    }

    /**
     * 减法
     *
     * @param var1
     * @param var2
     * @return
     */

    public static double sub(double var1, double var2) {
        BigDecimal b1 = new BigDecimal(Double.toString(var1));
        BigDecimal b2 = new BigDecimal(Double.toString(var2));
        return b1.subtract(b2).doubleValue();
    }

    /**
     * 乘法
     *
     * @param var1
     * @param var2
     * @return
     */
    public static double mul(double var1, double var2) {
        BigDecimal b1 = new BigDecimal(Double.toString(var1));
        BigDecimal b2 = new BigDecimal(Double.toString(var2));
        return b1.multiply(b2).doubleValue();
    }

    /**
     * 四舍五入
     *
     * @param v
     * @param scale 精确位数
     * @return
     */
    public static double round(double v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b = new BigDecimal(Double.toString(v));
        BigDecimal one = new BigDecimal("1");
        return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();

    }

}
