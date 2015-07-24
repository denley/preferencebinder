package me.denley.preferencebinder.internal;

public enum PrefType {
    // Defaults are arbitrary as we always check that a value exists
    BOOLEAN("boolean", "getBoolean", "false"),
    FLOAT("float", "getFloat", "0"),
    INTEGER("int", "getInt", "0"),
    LONG("long", "getLong", "0"),
    STRING("java.lang.String", "getString", "null"),
    STRING_SET("java.util.Set<java.lang.String>", "getStringSet", "null");

    private String fieldTypeDef;
    private String methodName;
    private String defaultValue;

    PrefType(String fieldTypeDef, String methodName, String defaultValue){
        this.fieldTypeDef = fieldTypeDef;
        this.methodName = methodName;
        this.defaultValue = defaultValue;
    }

    public String getFieldTypeDef() {
        return fieldTypeDef;
    }

    public String getSharedPrefsMethodName() {
        return methodName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
