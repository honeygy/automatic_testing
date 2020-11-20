package org.example;

import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @Author: Geng Yi
 * @Description: 处理cg节点/保存信息/得到.dot文件
 */
public class ChaDeal {
    private CHACallGraph cg;
    public ArrayList<Class_InterMethod> array;
    public ArrayList<ClassDepend> classDepends;
    public ArrayList<MethodDepend> methodDepends;


    public ChaDeal(CHACallGraph cg,String dotName) throws IOException{
        this.cg=cg;
        this.array=new ArrayList<Class_InterMethod>();
        this.classDepends=new ArrayList<ClassDepend>();
        this.methodDepends=new ArrayList<MethodDepend>();
        this.trans();
        //该方法得到对应的dot文件
        //this.getDotFiles(dotName);
    }
    private void getDotFiles(String dotName)throws IOException {
        //dotClassFile
        File dotClassFile = new File("./class-" + dotName + ".dot");
        if (!dotClassFile.exists()) {
            dotClassFile.createNewFile();
        }
        FileWriter writer = new FileWriter(dotClassFile);
        String content="digraph " + dotName + "_class {\n";
        for (ClassDepend depend : classDepends) {
            content+="\t\"" + depend.callee.getName().toString() + "\" -> \"" + depend.caller.getName().toString() + "\";\n";
        }
        content+="}";
        writer.append(content);
        writer.close();
        //dotMethodFile
        File dotMethodFile = new File("./method-" + dotName + ".dot");
        if (!dotMethodFile.exists()) {
            dotMethodFile.createNewFile();
        }
        writer = new FileWriter(dotMethodFile);
        content="digraph " + dotName + "_method {\n";
        for (MethodDepend depend : methodDepends) {
            content+="\t\"" + depend.callee.getSignature() + "\" -> \"" + depend.caller.getSignature() + "\";\n";
        }
        content+="}";
        writer.append(content);
        writer.close();

    }
    //构建生产代码与测试代码之间的联系
    private void trans(){
        for(CGNode node:cg){
            if(node.getMethod() instanceof ShrikeBTMethod){
                ShrikeBTMethod method=(ShrikeBTMethod) node.getMethod();
                if ("Application".equals(method.getDeclaringClass().getClassLoader().toString())){
                    //记录类和类内方法
                    boolean judge=false;
                    for(Class_InterMethod cm:array){
                        if(cm.iclass.equals(method.getDeclaringClass())){
                            cm.addMethod(method);
                            judge=true;
                            break;
                        }
                    }
                    if(!judge){
                        Class_InterMethod cm=new Class_InterMethod(method.getDeclaringClass());
                        cm.addMethod(method);
                        array.add(cm);
                    }

                    //记录依赖
                    Iterator<CGNode> predNodes=cg.getPredNodes(node);
                    while(predNodes.hasNext()){
                        CGNode predNode=predNodes.next();
                        if(predNode.getMethod() instanceof ShrikeBTMethod){
                            ShrikeBTMethod caller=(ShrikeBTMethod) predNode.getMethod();
                            if ("Application".equals(caller.getDeclaringClass().getClassLoader().toString())) {
                                judge = false;
                                for (ClassDepend depend : classDepends) {
                                    if (depend.caller.equals(caller.getDeclaringClass()) && depend.callee.equals(method.getDeclaringClass())) {
                                        judge = true;
                                    }
                                }
                                if (!judge) {
                                    classDepends.add(new ClassDepend(caller.getDeclaringClass(), method.getDeclaringClass()));
                                }
                                judge = false;
                                for (MethodDepend depend : methodDepends) {
                                    if (depend.caller.equals(caller) && depend.callee.equals(method)) {
                                        judge = true;
                                    }
                                }
                                if (!judge) {
                                    methodDepends.add(new MethodDepend(caller, method));
                                }
                            }

                        }
                    }
                }
            }

        }
    }

}
