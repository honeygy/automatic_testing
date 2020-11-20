package org.example;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeBTMethod;

import java.util.ArrayList;
/**
 * @Author: Geng Yi
 * @Description: 类及类内方法
 */
public class Class_InterMethod {
    public IClass iclass;
    public ArrayList<ShrikeBTMethod> methods;

    public void addMethod(ShrikeBTMethod method) {
        if (this.methods.indexOf(method) == -1)
            this.methods.add(method);
    }
    public Class_InterMethod(IClass iclass) {
        this.iclass = iclass;
        this.methods = new ArrayList<ShrikeBTMethod>();
    }
}
