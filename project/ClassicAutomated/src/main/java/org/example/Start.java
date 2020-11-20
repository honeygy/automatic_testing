package org.example;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @Author: Geng Yi
 * @Description: 处理参数和调用方法
 */
public class Start {
    private static AnalysisScope scope;
    //递归读取target里面.class文件
    private static void readClassFiles(String path)throws InvalidClassFileException{
        File file = new File(path);
        assert file.exists();//否则会抛出AssertionError
        File[] files = file.listFiles();
        if(files==null) {//长度为0...
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                readClassFiles(f.getAbsolutePath());
            }else if (f.getName().endsWith(".class")) {
                scope.addClassFileToScope(ClassLoaderReference.Application, f);
            }
        }

    }
    //判断是否是测试方法
    private static boolean isTestMethod(ShrikeBTMethod method){
        Collection<Annotation> annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation.getType().getName().toString().equals("Lorg/junit/Test")) {
                return true;
            }
        }
        return false;
    }
    //输出结果文件
    private static void output(ArrayList<ShrikeBTMethod> result,boolean isClassLevel)throws IOException{
        File resultFile;
        if(isClassLevel){
             resultFile= new File("./selection-class.txt");
        }else{
            resultFile = new File("./selection-method.txt");
        }
        if (!resultFile.exists()) {
            resultFile.createNewFile();
        }
        FileWriter writer = new FileWriter(resultFile);
        String content="";
        for (ShrikeBTMethod method: result) {
            content+=method.getDeclaringClass().getName().toString()+" "+method.getSignature()+"\n";
        }
        writer.append(content);
        writer.close();
    }
    //解决方法间接依赖
    public static void findIndirectDepend(ArrayList<ShrikeBTMethod> result,ArrayList<ShrikeBTMethod> initialMethod,ArrayList<MethodDepend> methodDepends){
        ArrayList<ShrikeBTMethod> temp=new ArrayList<ShrikeBTMethod>();
        for(ShrikeBTMethod method:initialMethod){
            for(MethodDepend depend:methodDepends){
                if(method.equals(depend.callee)){
                    if(result.indexOf(depend.caller)==-1){
                        result.add(depend.caller);
                        if(initialMethod.indexOf(depend.caller)==-1){
                            temp.add(depend.caller);
                        }
                    }
                }
            }
        }
        if(temp.size()==0){
            return;
        }else {
            initialMethod.addAll(temp);
            findIndirectDepend(result,initialMethod,methodDepends);
        }
    }
    public static void main(String[] args)throws IOException, InvalidClassFileException, ClassHierarchyException, CancelException{
        //构建分析域对象scope
        File exclusion = new File("exclusion.txt");
        ClassLoader classLoader = Start.class.getClassLoader();
        scope = AnalysisScopeReader.readJavaScope("scope.txt", exclusion, classLoader);
        readClassFiles(args[1]);
        //读取change_info.txt
        File changeInfoFile = new File(args[2]);
        assert changeInfoFile.exists();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(changeInfoFile));
        StringBuffer stringBuffer = new StringBuffer();
        String line = bufferedReader.readLine();
        while (line != null) {
            stringBuffer.append(line + "\n");
            line = bufferedReader.readLine();
        }
        ArrayList<String> changeInfo = new ArrayList<String>(Arrays.asList(stringBuffer.toString().split("\n")));
        //生成类层次关系对象
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        //生成进入点
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
        //利于CHA算法构建调用图
        CHACallGraph cg = new CHACallGraph(cha);
        cg.init(eps);
        ChaDeal chaDeal=new ChaDeal(cg,"ALU");//记得更换文件名
        ArrayList<ShrikeBTMethod> result=new ArrayList<ShrikeBTMethod>();
        if(args[0].equals("-c")){
            ArrayList<String> classNames=new ArrayList<String>();//change_info类名
            ArrayList<IClass> testClass=new ArrayList<IClass>();//变更生产类caller
            for(int i=0;i<changeInfo.size();i++){//得先判断分割字符串两个...
                  if(classNames.indexOf(changeInfo.get(i).split(" ")[0])==-1){
                      classNames.add(changeInfo.get(i).split(" ")[0]);
                  }
            }
            for(ClassDepend depend:chaDeal.classDepends){
                if(classNames.indexOf(depend.callee.getName().toString())!=-1){
                    testClass.add(depend.caller);
                }
            }
            for(IClass iclass:testClass){
                for(Class_InterMethod cm:chaDeal.array){
                    if(cm.iclass.equals(iclass)){
                        for(ShrikeBTMethod method:cm.methods){
                            if(isTestMethod(method) && result.indexOf(method)==-1){
                                result.add(method);
                            }
                        }
                        break;
                    }
                }
            }
            output(result,true);
        }else{
            String className="",signature="";
            ArrayList<ShrikeBTMethod> initialMethod=new ArrayList<ShrikeBTMethod>();
            for(int i=0;i<changeInfo.size();i++){
                className=changeInfo.get(i).split(" ")[0];
                signature=changeInfo.get(i).split(" ")[1];
                for(MethodDepend depend:chaDeal.methodDepends){//变更生产方法callee
                    if(className.equals(depend.callee.getDeclaringClass().getName().toString()) && signature.equals(depend.callee.getSignature())){
                        initialMethod.add(depend.callee);
                    }
                }
            }
            findIndirectDepend(result,initialMethod,chaDeal.methodDepends);
            ArrayList<ShrikeBTMethod> temp=new ArrayList<ShrikeBTMethod>();
            for(ShrikeBTMethod method:result){
                if(isTestMethod(method)){
                    temp.add(method);
                }
            }
            result=temp;
            output(result,false);
        }
    }
}
