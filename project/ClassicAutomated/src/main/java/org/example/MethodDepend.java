package org.example;

import com.ibm.wala.classLoader.ShrikeBTMethod;

/**
 * @Author: Geng Yi
 * @Description: 方法依赖
 */
public class MethodDepend {
    public ShrikeBTMethod caller, callee;

    public MethodDepend(ShrikeBTMethod caller, ShrikeBTMethod callee) {
        this.caller = caller;
        this.callee = callee;
    }
}
