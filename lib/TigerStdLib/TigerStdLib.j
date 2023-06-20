;
;  CPSC 433, S'18
;  HW #6
;  Solution
;
.class TigerStdLib
.super java/lang/Object
.method public <init>()V
aload_0
invokespecial java/lang/Object/<init>()V
return
.end method
; end initial setup for class TigerStdLib
;
.method public static print(Ljava/lang/String;)V
.limit stack 2
.limit locals 1
getstatic java/lang/System/out Ljava/io/PrintStream;
aload_0
invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V
return
.end method
;


.method public static printi(I)V
.limit locals 1
.limit stack 2
getstatic java/lang/System/out Ljava/io/PrintStream;
iload_0
invokevirtual java/io/PrintStream/print(I)V
return
.end method
;
.method public static flush()V
.limit locals 0
.limit stack 0
invokestatic java/lang/System/out/flush()V 
return
.end method
;
.method public static getchar()Ljava/lang/String;
.limit locals 1
.limit stack 1
getstatic java/lang/System/in Ljava/io/InputStream;
invokevirtual java/io/InputStream/read()I
istore_0
iload_0
ifge iffalse$19
ldc ""
areturn
iffalse$19:
iload_0
i2c
invokestatic java/lang/String/valueOf(C)Ljava/lang/String;
areturn
.end method
;
.method public static ord(Ljava/lang/String;)I
.limit locals 2
.limit stack 2
aload_0
invokevirtual java/lang/String.length()I
ifne iffalse$1
iconst_m1
ireturn
iffalse$1:
aload_0
iconst_0
invokevirtual java/lang/String/charAt(I)C
ireturn
.end method
;
.method public static chr(I)Ljava/lang/String;
.limit locals 1
.limit stack 1
iload 0
i2c
invokestatic java/lang/String/valueOf(C)Ljava/lang/String;
areturn
.end method
;
.method public static size(Ljava/lang/String;)I
.limit locals 1
.limit stack 1
aload_0
invokevirtual java/lang/String/length()I
ireturn
.end method
;
.method public static substring(Ljava/lang/String;II)Ljava/lang/String;
.limit locals 3
.limit stack 4
aload 0
iload 1
iload 1
iload 2
iadd
invokevirtual java/lang/String/substring(II)Ljava/lang/String;
areturn
.end method
;
.method public static concat(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
.limit locals 2
.limit stack 2
aload 0
aload 1
invokevirtual java/lang/String/concat(Ljava/lang/String;)Ljava/lang/String;
areturn
.end method
;
.method public static not(I)I
.limit locals 1
.limit stack 1
iload_0
ifne iffalse$20
iconst_1
ireturn
iffalse$20:
iconst_0
ireturn
.end method
;
.method public static exit(I)V
.limit locals 1
.limit stack 1
iload 0
invokestatic java/lang/System/exit(I)V 
return
.end method
;
