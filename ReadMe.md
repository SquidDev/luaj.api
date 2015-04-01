# LuaJ APIs [![Build Status](https://travis-ci.org/SquidDev/luaj.api.svg?branch=master)](https://travis-ci.org/SquidDev/luaj.api)
Nicer APIs for LuaJ
 
## Usage
```java
@LuaAPI("thing")
public class Thing {
	@LuaFunction
	public int add(int a, int b) {
		// Automatic validation and casting
		return a + b;
	}
	
	@LuaFunction
	public Varargs invoke(LuaTable table, LuaValue key, Varargs args) {
		// Varargs and LuaValue support
		return table.get(key).invoke(args);
	}
	
	@LuaFunction({"getColors", "getColours"})
	public String[] getColours() {
		// Rename functions
		// Arrays are returned as tables
		return new String[] {"red", "green", "blue"}
	}
	
	@LuaFunction(isVarArgs = true)
	public String[] getColoursTable() {
		// But can be used as Varargs
		// It is still a normal Java class
		return getColours();
	}
	
	@LuaFunction
	@ValidationClass(StrictValidator.class)
	public void strict(String key) {
		// Strict validation will only accept Strings, 
		// it won't cast numbers to strings for instance
		
		// This can be applied on the whole class, 
		// the method or on one argument
	}
}
```
