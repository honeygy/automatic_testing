package org.example;

import com.ibm.wala.classLoader.IClass;

/**
 * @Author: Geng Yi
 * @Description: 类依赖
 */
public class ClassDepend {
    public IClass caller, callee;

    public ClassDepend(IClass caller, IClass callee) {
        this.caller = caller;
        this.callee = callee;
    }
}
