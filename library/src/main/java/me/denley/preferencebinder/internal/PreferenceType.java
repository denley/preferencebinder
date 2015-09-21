package me.denley.preferencebinder.internal;

public enum PreferenceType {
    // Defaults are arbitrary as we always check that a value exists
    BOOLEAN("boolean", "getBoolean", "putBoolean", "false"),
    FLOAT("float", "getFloat", "putFloat", "0"),
    INTEGER("int", "getInt", "putInt", "0"),
    LONG("long", "getLong", "putLong", "0"),
    STRING("java.lang.String", "getString", "putString", "null"),
    STRING_SET("java.util.Set<java.lang.String>", "getStringSet", "putStringSet", "null");

    public static PreferenceType getType(String typeDef){
        if(typeDef == null){
            return null;
        }

        for(PreferenceType type: PreferenceType.values()){
            if(type.getFieldTypeDef().equals(typeDef)){
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid preference value type ("+typeDef+")");
    }



    private String fieldTypeDef;
    private String getterMethod;
    private String setterMethod;
    private String defaultValue;

    PreferenceType(String fieldTypeDef, String getterMethod, String setterMethod, String defaultValue){
        this.fieldTypeDef = fieldTypeDef;
        this.getterMethod = getterMethod;
        this.setterMethod = setterMethod;
        this.defaultValue = defaultValue;
    }

    public String getFieldTypeDef() {
        return fieldTypeDef;
    }

    public String getSharedPrefsGetterMethod() {
        return getterMethod;
    }

    public String getSetterMethod() {
        return setterMethod;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

}
