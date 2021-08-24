# YY Android 编码规范

## 1 前言

这份文档是YY Android编程风格规范的完整定义。当且仅当一个Java/Kotlin源文件符合此文档中的规则，我们才认为它符合YY的Android编程风格。

与其它的编程风格指南一样，这里所讨论的不仅仅是编码格式美不美观的问题， 同时也讨论一些约定及编码标准。然而，这份文档主要侧重于我们所普遍遵循的规则， 对于那些不是明确强制要求的，我们尽量避免提供意见。

### 1.1 术语说明

在本文档中，除非另有说明：

1. 术语class可表示一个普通类，枚举类，接口或是annotation类型(`@interface`).
1. 术语comment只用来指代实现的注释(implementation comments)，我们不使用“documentation comments”一词，而是用Javadoc。

其他的术语说明会偶尔在后面的文档出现。

### 1.2 指南说明

本文档中的示例代码并不作为规范。也就是说，虽然示例代码是遵循编程风格，但并不意味着这是展现这些代码的唯一方式。 示例中的格式选择不应该被强制定为规则。

### 1.3 规范说明

本规范中的条例未标明kotlin或java特有时,表示kotlin及java都需要遵守.

---

## 2 源文件基础

### 2.1 文件名

源文件以其最顶层的类名来命名，大小写敏感，java文件扩展名为.java, kotlin文件扩展名为.kt

### 2.2 文件编码: UTF-8

源文件编码格式为UTF-8.

### 2.3 特殊字符

#### 2.3.1 空白字符

除了行结束符序列， **ASCII水平空格字符(0x20，即空格) (0x20)** 是源文件中唯一允许出现的空白字符，这意味着：

* 所有其它字符串中的空白字符都要进行转义。
* 制表符 **不** 用于缩进。

#### 2.3.2 特殊转义序列

对于具有特殊转义序列的任何字符 (`\b`, `\t`, `\n`, `\f`, `\r`, `\"`, `\'` 和`\\`), 我们使用它的转义序列，而不是相应的八进制 (比如 `\012`) 或 Unicode (比如 `\u000a`) 转义.

## 3 源文件结构

一个源文件包含 **（按顺序地）**:

* Package 语句
* Import 语句
* 顶级类(java只有一个、kotlin可以多个)

以上每个部分之间用一个空行隔开。

### 3.1 Package 语句

package 语句不换行. 列限制 ( 4.4 节, 列限制: 120) 并不适用于package语句。(即package语句写在一行里)

### 3.2 Import 语句

#### 3.2.1 import不要使用通配符

通配符imports, 静态的或者其他的, 都不能使用.

#### 3.2.2 不要换行

Import 语句不换行. 列限制 (4.4节, 列限制: 120) 并不适用于import语句。(每个import语句独立成行)

### 3.3 类声明

#### 3.3.1 顶级类声明

##### java特有
只有一个顶级类声明

每个顶级类都在一个与它同名的源文件中(当然，还包含.java后缀)。例外：package-info.java，该文件中可没有package-info类。

##### kotlin特有
可以有多个顶级类声明
如果包含多个类,选择一个能够描述该文件的类名作为文件名

---

## 4 格式

**术语说明:** *块状结构（block-like construct）* 指的是一个类，方法或构造函数的主体。 需要指明的是, by Section 4.8.3.1 on 数组初始化, 数组初始化中的初始值 可 被选择性地视为块状结构

### 4.1 大括号

#### 4.1.1 使用大括号(即使是可选的)

大括号应与 `if`, `else`, `for`, `do` 还有 `while` 语句一起使用,即使只有一条语句，也应该把大括号写上。

#### 4.1.2 非空块: K & R 风格

大括号遵循 Kernighan 和 Ritchie 风格 ("[Egyptian brackets](http://www.codinghorror.com/blog/2012/07/new-programming-jargon.html)") 对于 非空 块和块状结构来说:

* 左大括号前不换行
* 左大括号后换行
* 右大括号前换行
* 如果右大括号是一个语句、函数体或类的终止，则右大括号后换行; 否则不换行。例如，如果右* 大括号后面是`else`或是逗号，则不换行

例如:

```java
return new MyClass() {
    @Override public void method() {
        if (condition()) {
            try {
                something();
            } catch (ProblemException e) {
                recover();
            }
        }
    }
};
```

在 4.8.1节,枚举类给出了`enum`类的一些例外。

#### 4.1.3 空块：可以用简洁版本

一个空的块状结构里什么也不包含，大括号可 简洁地写成 ({}), 除非它是 多块语句 的一部分，(一个直接包含多块: if/else-if/else or try/catch/finally).

示例:

```java
void doNothing() {}
```

### 4.2 块缩进：4个空格

每当开始一个新的块，缩进增加4个空格，当块结束时，缩进返回先前的缩进级别。缩进级别适用于代码和注释。(见 4.1.2节中的代码示例, [非空块: K & R 风格](file:///D:/svnwork/ent/entmobile-android_7.0.0_maint/styleguide/javaguide.html#s4.1.2-blocks-k-r-style).)

### 4.3 一行一个语句

每个语句后要换行。

### 4.4 列限制: 120

一个项目可以选择一行120个字符的列限制，除了下述例外，任何一行如果超过这个字符数限制，必须自动换行。见4.5节，自动换行.

**例外:**

1. 不可能满足列限制的行(例如，Javadoc中的一个长URL，或是一个长的JSNI方法参考)。
1. `package` 和 `import` 语句 (见 3.2节 Package 语句 和 3.3节Import 语句).

### 4.5 空白

#### 4.5.1 垂直空白

以下情况需要使用一个空行：

1. 类内连续的成员之间：字段，构造函数，方法，嵌套类，静态初始化块，实例初始化块。在函数体内，语句的逻辑分组间使用空行。
    * 例: 两个连续字段之间的空行是可选的，用于字段的空行主要用来对字段进行逻辑分组。
1. 在方法体中，按需创建逻辑分组
1. 类内的第一个成员前或最后一个成员后的空行是可选的(既不鼓励也不反对这样做，视个人喜好而定)。
1. 要满足本文档中其他节的空行要 (例如3.3节, Import 语句).

多个连续的空行是允许的，但没有必要这样做(我们也不鼓励这样做)。

#### 4.5.2 水平空白

除了语言需求和其它规则，并且除了文字，注释和Javadoc用到单个空格，单个ASCII空格也出现在以下几个地方：

1. 分隔任何保留字与紧随其后的左括号 (`(`) （如 `if`, `for`, `catch`等)
1. 分隔任何保留字与其前面的右大括号 (`}`) （如 `else` 或 `catch`）
1. 在任何左大括号前 (`{`), 有两个例外:
    * `@SomeAnnotation({a, b})` (不使用空格)
    * `String[][] x = {{"foo"}};` (在`{{`之间下的8项没有空格)
1. 不要在一元运算符左右留空格（a++）
1. 在任何二元或三元运算符的两侧。这也适用于以下“类运算符”符号：
    * 类型界限中的&： `<T extends Foo & Bar>`
    * catch块中的管道符号： `catch (FooException | BarException e)`
    * 分号(`:`) 存在于 ("foreach") 语句
1. 在 `,:;` 或右括号 (`)`) 的后面;`;`作为非语句结束符时,后面加空格
1. 类型和变量之间： `List<String> list`
1. 不要在 :: 前后留空格：Foo::class、 String::length
1. 不要在用于标记可空类型的 ? 前留空格：String?

> **注:** 这个规则并不要求或禁止一行的开关或结尾需要额外的空格，只对内部空格做要求。


### 4.6 具体结构

#### 4.6.1 枚举类

枚举常量间用逗号隔开，换行可选。

没有方法和文档的枚举类可写成数组初始化的格式(见 4.8.3.1节 数组初始化).

```java
private enum Suit { CLUBS, HEARTS, SPADES, DIAMONDS }
```

由于枚举类也是一个类，因此所有适用于其它类的格式规则也适用于枚举类。

#### 4.6.2 变量声明

##### 4.6.2.1 每次只声明一个变量

不要使用组合声明，例如 `int name, head`; // !!!

##### 4.6.2.2 需要时才声明，并进行初始化

不要在一个代码块的开头把局部变量一次性都声明了(这是c语言的做法)，而是在第一次需要使用它时才声明。 局部变量在声明时必须进行初始化。

#### 4.6.3 数组

##### 4.6.3.2 非C风格的数组声明

中括号是类型的一部分： `String[] args`, 而非 `String args[]`.

#### 4.6.4 Switch 语句

术语说明： switch块的大括号内是一个或多个语句组。每个语句组包含一个或多个switch标签 ( `case FOO:` 或 `default:`), 后面跟着一条或多条语句。

##### 4.6.4.1 缩进

与其它块状结构一致，switch块中的内容缩进为4个空格。

每个switch标签后新起一行，再缩进4个空格，写下一条或多条语句。

##### 4.6.4.2 块结束

在一个switch块内，每个语句组要么通过 `break`, `continue`, `return` 或抛出异常来终止)。示例：

```java
switch (input) {
    case 1:
    case 2:
        prepareOneOrTwo();
        // bad
    case 3:
        handleOneTwoOrThree();
        break;
    default:
        handleLargeNumber(input);
}
```

##### 4.6.4.3 default的情况要写出来

每个switch语句都包含一个 `default` 语句组，即使它什么代码也不包含。default语句放在switch语句快的末端.

#### 4.6.5 注解(Annotations)

注解紧跟在文档块后面，应用于类、方法和构造函数，一个注解独占一行。这些换行不属于自动换行( 4.5节, 自动换行), 因此缩进级别不变。例如：

```java
@Override
@Nullable
public String getNameIfPresent() { ... }
```

#### 4.6.6 注释

##### 4.6.6.1 块注释风格

块注释与其周围的代码在同一缩进级别。它们可以是 `/* ... */` 风格，也可以是 `// ...` 风格.对于多行的 `/* ... */` 注释, 后续行必须从 `*` 开始， 并且与前一行的 `*` 对齐。以下示例注释都是OK的。

```java
/*
 * This is          // And so
 * okay.            // is this.
 */
```

注释不要封闭在由星号或其它字符绘制的框架里。

> **提示：**在写多行注释时，如果你希望在必要时能重新换行(即注释像段落风格一样)，那么使用 `/* ... */` 风格，如果你想必要时段落自动代码格式化，大多数代码没有重新包装，就使用 `// ...` 风格.

#### 4.6.7 数值型的字面值

`long`-使用大写字母 `L` 后缀, 没有小写 (避免与数字 `1` 混淆). 例如, `3000000000L` 而不是 `3000000000l`.

### 4.7 kotlin特有
#### 4.7.1 类头格式化
 * 具有少数主构造函数参数的类可以写成一行：

```
    class Person(id: Int, name: String)
```

 * 具有较长类头的类应该格式化，以使每个主构造函数参数都在带有缩进的独立的行中。 另外，右括号应该位于一个新行上。如果使用了继承，那么超类的构造函数调用或者所实现接口的列表应该与右括号位于同一行：

```java
    class Person(
        id: Int,
        name: String,
        surname: String
    ) : Human(id, name) { …… }
```

 * 对于多个接口，应该将超类构造函数调用放在首位，然后将每个接口应放在不同的行中：

```java
    class Person(
        id: Int,
        name: String,
        surname: String
    ) : Human(id, name),
    KotlinMaker { …… }
```

 * 对于具有很长超类型列表的类，在冒号后面换行，并横向对齐所有超类型名：

```java
    class MyFavouriteVeryLongClassHolder :
        MyLongHolder<MyFavouriteVeryLongClass>(),
        SomeOtherInterface,
        AndAnotherOne {

        fun foo() { ... }
    }
```

 * 构造函数参数使用常规缩进（4 个空格）。

        理由：这确保了在主构造函数中声明的属性与 在类体中声明的属性具有相同的缩进。


 * 由单个表达式构成的函数体，优先使用表达式形式。

```java
    fun foo(): Int {     // 不良
        return 1
    }

    fun foo() = 1        // 良好
```

#### 4.7.2 Lambda 表达式格式化
在 lambda 表达式中，应该在花括号左右以及分隔参数与代码体的箭头左右留空格。 如果一个调用接受单个 lambda 表达式，应该尽可能将其放在圆括号外边传入。

```java
    list.filter { it > 10 }
```

如果为 lambda 表达式分配一个标签，那么不要在该标签与左花括号之间留空格：

```java
    fun foo() {
        ints.forEach lit@{
            // ……
        }
    }
```

在多行的 lambda 表达式中声明参数名时，将参数名放在第一行，后跟箭头与换行符：

```java
    appendCommaSeparated(properties) { prop ->
        val propertyValue = prop.get(obj)  // ……
    }
```

如果参数列表太长而无法放在一行上，请将箭头放在单独一行：

```java
    foo {
        context: Context,
        environment: Env
        ->
        context.configureEnv(environment)
    }
```

#### 4.7.3 Unit
如果函数返回 Unit，那么应该省略返回类型：

```java
    fun foo() { // 这里省略了“: Unit”

    }
```

#### 4.7.4 字符串模版
将简单变量传入到字符串模版中时不要使用花括号。只有用到更长表达式时才使用花括号。

```java
    KLog.i(TAG ,"$name has ${children.size} children")
```

---

## 5 命名约定

### 5.1 对所有标识符都通用的规则

标识符只能使用ASCII字母和数字，因此每个有效的标识符名称都能匹配正则表达式 \w[\w\d]+ .

在其它编程语言风格中使用的特殊前缀或后缀，如 `name_`, `s_name`, `kName`, 在Java编程风格中都 **不** 再使用.

下划线规则不适用于常量定义.

### 5.2 标识符类型的规则

#### 5.2.1 包名

包名全部小写，连续的单词只是简单地连接起来，不使用下划线。例如， `com.example.deepspace`, 不是 `com.example.deepSpace` 或者 `com.example.deep_space`.

#### 5.2.2 类名

类名都以UpperCamelCase驼峰式命名法风格编写。

类名通常是名词或名词短语，例如 `Character` 或 `ImmutableList`. 接口名称有时可能是名词或名词短语(例如, `List`),也有时可能是形容词或形容词短语 (例如, `Readable`).

现在还没有特定的规则或行之有效的约定来命名注解类型。

测试 类的命名以它要测试的类的名称开始， 以 `Test` 结束. 例如, `HashTest` 或 `HashIntegrationTest`.

#### 5.2.3 方法名

方法名都以 lowerCamelCase 编写.

方法名通常是动词或动词短语。 例如, `sendMessage` 或 `stop`.

下划线可能出现在JUnit测试方法名称中用以分隔名称的逻辑组件。一个典型的模式是： `test<MethodUnderTest>_<state>`, 例如`testPop_emptyStack`.并不存在唯一正确的方式来命名测试方法。

#### 5.2.4 常量名

常量名命名模式为 `CONSTANT_CASE`: 全部字母大写，用下划线分隔单词。那，到底什么算是一个常量？

每个常量都是一个静态final字段，但不是所有静态final字段都是常量。在决定一个字段是否是一个常量时， 考虑它是否真的感觉像是一个常量。例如，如果任何一个该实例的观测状态是可变的，则它几乎肯定不会是一个常量。 只是永远不打算改变对象一般是不够的，它要真的一直不变才能将它示为常量。 如:

```java
// Constants
static final int NUMBER = 5;
static final ImmutableList<String> NAMES = ImmutableList.of("Ed", "Ann");
static final Joiner COMMA_JOINER = Joiner.on(',');  // because Joiner is immutable
static final SomeMutableType[] EMPTY_ARRAY = {};
enum SomeEnum { ENUM_CONSTANT }

// Not constants
static String nonFinal = "non-final";
final String nonStatic = "non-static";
static final Set<String> mutableCollection = new HashSet<String>();
static final ImmutableSet<SomeMutableType> mutableElements = ImmutableSet.of(mutable);
static final Logger logger = Logger.getLogger(MyClass.getName());
static final String[] nonEmptyArray = {"these", "can", "change"};
```

这些名字通常是名词或名词短语。

#### 5.2.5 成员变量的命名规则

* 成员变量命名直接以小写字母开始, 以lowerCamelCase风格编写.

#### 5.2.6 参数名

参数名以lowerCamelCase风格来编写.

参数禁止用单个字符命名。

#### 5.2.7 局部变量名

局部变量名以lowerCamelCase风格编写, 比起其它类型的名称，局部变量名可以有更为宽松的缩写。

虽然缩写更宽松，但还是禁止用单字符进行命名。

即使局部变量是final和不可改变的，也不应该把它示为常量，自然也不能用常量的规则去命名它。

## 6 其他

### 6.1 捕获的异常：不能忽视

除了下面的例子，对捕获的异常不做响应是极少正确的。(典型的响应方式是打印日志，或者如果它被认为是不可能的，则把它当作一个 `AssertionError` 重新抛出)

如果它确实是不需要在catch块中做任何响应，需要做注释加以说明(如下面的例子)。

```java
try {
    int i = Integer.parseInt(response);
    return handleNumericResponse(i);
} catch (NumberFormatException ok) {
    // it's not numeric; that's fine, just continue
}
return handleTextResponse(response);
```

**例外:**在测试中，如果一个捕获的异常被命名为expected. 则它可以被不加注释地忽略。下面是一种非常常见的情形，用以确保所测试的方法会抛出一个期望中的异常， 因此在这里就没有必要加注释。

```java
try {
    emptyStack.pop();
    fail();
} catch (NoSuchElementException expected) {
}
```

### 6.2 静态成员：使用类进行调用

使用类名调用静态的类成员，而不是具体某个对象或表达式。

```java
Foo aFoo = ...;
Foo.aStaticMethod(); // good
aFoo.aStaticMethod(); // bad
somethingThatYieldsAFoo().aStaticMethod(); // very bad
```

### 6.3 Finalizers: 禁用

极少会去重载 `Object.finalize`.

> **提示：**不要使用finalize。如果你非要使用它，请先仔细阅读和理解 [Effective Java](http://books.google.com/books?isbn=8131726592) 第7条款, "Avoid Finalizers," 然后不要使用它。
