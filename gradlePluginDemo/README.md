# gradlePluginDemo
简单的gradle 插件demo，实现了注解修饰的方法，编译时自动插桩。
具体功能为，统计被注解的相关方法的执行耗时。

因gradle 最近的版本已经集成了asm等组件，因为并未引入asm插件库，也无需配置META-INF/gradle-plugins/xx.properties。
节省了冗繁的配置过程及代码
