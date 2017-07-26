package com.xyzq.simpson.maggie.logic;import com.xyzq.simpson.base.json.JSONArray;import com.xyzq.simpson.base.json.JSONObject;import com.xyzq.simpson.base.json.JSONVisitor;import com.xyzq.simpson.base.json.core.IJSON;import com.xyzq.simpson.base.model.core.IModule;import com.xyzq.simpson.base.runtime.Invoke;import com.xyzq.simpson.base.runtime.Method;import com.xyzq.simpson.base.type.safe.Table;import com.xyzq.simpson.maggie.Maggie;import java.util.Collection;import java.util.Map;/** * JS内置桥接服务 */public class BridgeService implements IModule {    /**     * 声明映射     */    private Table<String, InvokeFilter> declareTable = null;    /**     * 当前生效的类声明与泛型类声明映射     */    private ThreadLocal<Table<String, InvokeFilter>> currentDeclare = null;    /**     * 初始化     *     * @return 是否初始化成功     */    @Override    public boolean initialize() {        declareTable = new Table<String, InvokeFilter>();        currentDeclare = new ThreadLocal<Table<String, InvokeFilter>>();        return true;    }    /**     * 终止     */    @Override    public void terminate() {        declareTable = null;        currentDeclare.remove();        currentDeclare = null;    }    /**     * 声明调用方法     *     * @param serviceName 服务名称     * @param methodDeclare 方法声明语句     */    public void declare(String serviceName, String methodDeclare) {        if(null == serviceName) {            currentDeclare.remove();            return;        }        InvokeFilter invokeFilter = declareTable.get(serviceName + "." + methodDeclare);        if(null == invokeFilter) {            invokeFilter = InvokeFilter.build(methodDeclare);            declareTable.put(serviceName + "." + methodDeclare, invokeFilter);        }        Table<String, InvokeFilter> declareMapping = currentDeclare.get();        if(null == declareMapping) {            declareMapping = new Table<String, InvokeFilter>();            currentDeclare.set(declareMapping);        }        declareMapping.put(serviceName + "." + invokeFilter.methodName() + "." + invokeFilter.parameterCount(), invokeFilter);    }    /**     * JS调用     *     * @param serviceName 服务名称     * @param methodName 方法名称     * @param parameters 实参列表     * @return 调用结果     */    public Object invoke(String serviceName, String methodName, Object[] parameters) {        for (int i = 0; i < parameters.length; i++) {            if (null != parameters[i]) {                if (Double.class.equals(parameters[i].getClass())) {                    double doubleValue = ((Double) parameters[i]).doubleValue();                    if (doubleValue % 1.0D > 0.0D) {                        parameters[i] = Double.valueOf(doubleValue);                    }                    else {                        parameters[i] = Integer.valueOf((int) doubleValue);                    }                }                else if (Map.class.isAssignableFrom(parameters[i].getClass())) {                    parameters[i] = JSONObject.convertFromTable((Map) parameters[i]);                }                else if (Collection.class.isAssignableFrom(parameters[i].getClass())) {                    parameters[i] = JSONArray.convertFromSet((Collection) parameters[i]);                }            }        }        Invoke invoke = null;        Table<String, InvokeFilter> invokeFilterTable = currentDeclare.get();        if(null != invokeFilterTable) {            InvokeFilter invokeFilter = invokeFilterTable.get(serviceName + "." + methodName + "." + parameters.length);            if(null != invokeFilter) {                invoke = invokeFilter.filter(parameters);            }        }        if(null == invoke) {            invoke = new Invoke();            invoke.method = new Method();            invoke.method.name = methodName;            invoke.method.parameterTypes = new Class[parameters.length];            invoke.parameters = new Object[parameters.length];            for (int i = 0; i < parameters.length; i++) {                if (null == parameters[i]) {                    invoke.parameters[i] = null;                    invoke.method.parameterTypes[i] = String.class;                }                else {                    invoke.parameters[i] = parameters[i];                    invoke.method.parameterTypes[i] = parameters[i].getClass();                    if (Integer.class.equals(invoke.method.parameterTypes[i])) {                        invoke.method.parameterTypes[i] = Integer.TYPE;                    }                }            }        }        try {            return serialize(Maggie.invokeService().invoke(serviceName, invoke));        }        catch (Exception e) {            throw new RuntimeException(String.format("invoke service failed:\nservice = %s\ninvoke = %s", new Object[]{serviceName, invoke}), e);        }    }    /**     * Java对象转JS桥接对象     *     * @param javaObject Java对象     * @return JS桥接对象     */    private String serialize(Object javaObject) {        if (null == javaObject) {            return null;        }        if (IJSON.class.isAssignableFrom(javaObject.getClass())) {            return javaObject.toString();        }        if (Boolean.class.isAssignableFrom(javaObject.getClass())) {            return javaObject.toString().toLowerCase();        }        if (Number.class.isAssignableFrom(javaObject.getClass())) {            return javaObject.toString();        }        if (String.class.isAssignableFrom(javaObject.getClass())) {            return "'" + ((String) javaObject).replace("'", "'") + "'";        }        return JSONVisitor.convert(javaObject).toString();    }}