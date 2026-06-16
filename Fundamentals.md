# Fundamentals for understanding how MiniPool works

## 1) Race condition

A race condition basically happens when two or more threads try to access and modify the same resource at the same time.
The result depends on the timing of the execution, so as we can understand, the result can be unpredictable. Imagine
the following code:

```java
public class Counter{
    private int count = 0;
    
    public void increment(){
        count++;
    }
    public int getCount(){
        return count;
    }
}
```
Now let's suppose that two threads are trying to increment the counter at the same time. Essentially, they are calling
counter.increment() at the same time. At first glance, we might think that the counter will be incremented twice:

```text
count = 0;

Thread A increments -> count = 1;
Thread B increments -> count = 2; 

Final count = 2;
```
But in reality, `count++` is NOT a single operation. The JVM is actually executing:

```text
1. Read count
2. Add 1
3. Write count 
```
Now imagine that the JVM is executing the above operations using two threads:
```text
count = 0

Thread A reads count -> count = 0

(Thread switch)

Thread B reads count -> count = 0
Thread B adds 1 -> count = 1
Thread B writes count -> count = 1

(Thread switch)

Thread A adds 1 -> count = 1
Thread A writes count -> count = 1

Final count = 1
```
In the above example, the final count turns out to be 1. We therefore have a **lost update**, which is a type of race condition.

### Why is this important for MiniPool?

Imagine the following scenario:

```java
    if (totalCount < maxSize){
    totalCount++;
    openConnection();
    }
```
Suppose that maxSize is 10 and totalCount is 9 and two threads arrive at the same time:

```text
Thread A checks: 9 < 10
Thread B checks: 9 < 10

Thread A increments totalCount and opens a connection -> 10
Thread B increments totalCount and opens a connection -> 11
```
We will end up with 11 connections even though the limit is 10.

## 2) `synchronized` 

`synchronized` is a keyword which is basically one of java's mechanisms to deal with race conditions.
The main idea behind `synchronized` is mutual exlusion, which means that only one thread can execute a synchronized block
at a time for a given lock. Let's consider the following example:

```java
public class Counter{  
    int count = 0;
    
    public synchronized void increment(){
        count++;
    }
    
    public int getCount(){
        return count;
    }
}
```

When a thread calls the increment method, it acquires the lock which is associated with the current object (`this`).
Only one thread can acquire the lock at a time. If another thread tries to acquire the lock on the same object,
it must wait until the lock is released, or in other words, until the first thread finishes its execution. The JVM
behaves similarly to the following code:

```java
acquireLock(this);

try{
    count++;
} finally{
    releaseLock(this);
}
```

Suppose two threads try to increment the counter on a given object at the same time:

```text
Thread A calls increment
Thread B calls increment

The JVM execution would be the following: 

1. Thread A acquires the lock
2. Thread B tries to acquire the lock -> waits

3. Thread A increments the counter
4. Thread A releases the lock

5. Thread B acquires the lock
6. Thread B increments the counter
7. Thread B releases the lock
```

Because only one thread can execute the increment method at a time, the race condition is avoided and no updates are lost.
It is very important to note that the lock belongs to the object instance. Therefore, if we have two different objects, 
the following calls will not block each other because they use different locks: 

```text
Counter c1 = new Counter();
Counter c2 = new Counter();

c1.increment();
c2.increment();
```

However, if multiple threads try to access the same `Counter` instance, they will syncrhonize on the same lock.

While `synchronized` is simple and effective, it introduces some overhead. It introduces blocking behavior because threads
may need to wait for the lock to become available. This can lead to a performance bottleneck. This limitation is why
`synchronized` is not widely used and other mechanisms are preferred for concurrent programming. We discuss them in the following sections.

## 3) `volatile`

TBW

## 4) `AtomicInteger`

TBW

## 5) Compare-And-Swap (CAS) 

TBW

## 6) `BlockingQueue`

TBW