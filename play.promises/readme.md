# Promises
Promises are a concept developed in Javascript to work with asynchronous operations. A
promise represents the result of an asynchronous operation, even if the result is not 
available yet. When the result is available, the client of the promise can be notified with
a callback.

The basic idea of a Promise is that it is a stand-in for the final result. When the promise is
created, the promise is _unresolved_. When the result, somewhere in the future, arrives, the Promise
is said to _resolve_. A resolve can be a failure or a success. So there are three state:

* UNRESOLVED
* SUCCESS
* FAILURE

Resolving can occur at any moment after the asynchronous task is started, that is, it is 
perfectly valid for a Promise to be resolved when it is returned.

To simplify the handling of the result, it is therefore common that the
result us always processed in a callback. That is, a Promise is not a Future. A Future is
intended to be waited upon until the result arrives. A Promise is almost always accessed
through its call back. For example:

    interface Foo {
       String string();
       int toInt(String s);
       double toDouble(int v);
    }
    
    Foo					foo;
    
    void bar() {
    	final Promise<String> p  =async.call( foo.string() );
    	p.onresolve( () -> {
    		System.out/println( p.get() );
    	});
    }

It is also possible to use the Promise as a Future, it also has a `get` method.

    String s = async.call( foo.string() ).get();
    
This is identical to:

    Promise<String> ps = async.call( foo.string() );
    int result = ps.get();
    
Then the following is the action diagram for these steps:

            foo            async            ps  
    string-->|               |
             |-get promise-->|
             |               |--create----->|
             |               |              |
    -get----------------------------------->|
             :               :              :
             |               |              |<===== resolve
             
## Sequencing
One of the primary problems with asynchronous programming is that it is often necessary
to program a sequence of steps. This can obviously be done with nesting. However, this
has two problems. First and foremost, exception handling becomes very complicated. Each
step must manage different failure modes. Second, each step tends to get indented, creating
the deeply nested _stair_ problem. 

Javascript promises solve this problem with the `then` method. The `then` method comes in two
forms:

    <R> Promise<R> then(Success<R,T> success)
    <R> Promise<R> then(Success<R,T>,Failure<T> failure)

The `Success` callback has the following prototype:

    Promise<Return> call(Promise<Value> promise) throws Exception;
    
The success callback must return a promise, this is called the _result promise_. In general, it 
performs some conversion for the the  following step. In an synchronous world, this is 
likely being done in an other async call since waiting is considered evil. The result Promise can 
also  be `null` if the callback has no actual result or it can be an already resolved Promise 
when it does not need asynchronicity. In the method declaration the type of the result promise
is `R`. For example, a callback that transforms a String to an Integer:

    Promise<Integer> success(Promise<String> p) { 
    	return async.call( foo.toInteger(p.get())); 
    }

The `then` method also returns a `Promise<R>`, however, this is by definition a different Promise 
from the Success callback. Think about it, when the `then` method returns, the 
Success callback has (very likely) not been called, so a new `Promise<T>` is created. When
the Success callback has been executed, the result promise becomes available and then 
used to register a callback that when that Promise becomes resolved, the result promise
also becomes resolved. If your head spins, that is ok. Lets set up a chain:

    1,5: double result = async.call( foo.string() )
    2:     .then( (Promise<String> p )  -> return async.call(foo.toInt(p.get()))
    3:     .then( (Promise<Integer> p ) -> return async.call(foo.toDouble(p.get()))
    4:     .get();
      
1. Call an async method `String string()`. The `call` method therefore returns a Promise<String> 
2. Register a Success callback that will return an Integer Promise.
3. Register a Success callback that will return a Double promise
4. Block, this is obviously no longer async programming
5. If step 1, 2, and 3 have executed over time, the `get()` method unblocks and returns the result.

This model clearly solves the _stair_ problem. The next problem is error handling. As was shown earlier,
there are 2 prototypes for the `then` method. The second prototype that has not yet been
discussed contains an additional Failure handler. The Failure handler is called whenever
there is a failure. However, it is not necessary provide a failure handler for each
`then` step, this is only necessary for the last step since failures are propagated.
The failure handler is the asynchronous variation on exception handlers.

    async.call( foo.string() )
      .then( (Promise<String> p )  -> return async.call(foo.toInt(p.get()))
      .then( (Promise<Integer> p ) -> return async.call(foo.toDouble(p.get()),
          (Promise<Integer> p) -> fail(p.getError()) );

## Deferred
The Promises API is purely for the clients of the API, the code that needs to react on
the asynchronous results. However, the Promise must also be resolved, and this API
is not available on the Promise for security reasons, by separating the API in a Deferred
class, the implementation that handles the asynchronous aspects can be protected from
clients.

The Deferred class is created by the implementation code that initiates an asynchronous activity:

    Deferred<String>		deferred = new Deferred<>();

There is a `getPromise()` method that returns the Promise. There is always a single Promise 
associated with a Deferred. The Deferred is normally stored in a data structure that is
available when the async result finally returns. When the result returns, the `resolve()` method
is called on the Deferred, ensuring that the current and future registered callbacks are
executed.

For example, a small program that sends data to itself:

    Promise<String> p = selfie.send("Hello");
    assertEquals( "Hello", p.get());

We can setup such an example as follows.

    @Component
    public class Selfie extends Thread {
      DataOutputStream       dout;
      DataInputStream        din;
      AtomicInteger          ids = new AtomicInteger(1000);
	  Map<Integer,Deferred<String>> map = new ConcurrentHashMap<>();

In this example, a PipedInput/OutputStream is created. This is written from a `send` method
and read from a background thread.

      @Activate
      void activate() {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream out = new PipedOutputStream(in);
        dout = new DataOutputStream(out);
        din = new DataInputStream(in);
        start();
      }

To send a message, we create a unique id and a `Deferred` object. These are remembered
in a map so that we can find the deferred if the map arrives in the listening thread.
The message is just written to the other side.
      
      public synchronized Promise<String> send(String s) throws Exception {
        int id = i.getAndIncrement();
		Deferred<String> d = new Deferred<>();
        map .put(id,d);
        dout.writeInt(id); dout.writeUTF(s); dout.flush();
        return d.getPromise();
      }

The `run` method reads the input stream as long as there is data. (We assume in here that 
messages are shorter than the buffers used.) When a message arrives we get its id, look
up the associated Deferred, and the resolve the deferred with the upper case version
of the associated string.
      
      public void run() {
        try {
          while(!isInterrupted()) {
            int id = din.readInt();
            String s = din.readUTF();
            Deferred<String> d = map.remove(id);
            d.resolve(s.toUpperCase());
          }
        } catch( Exception e) {e.printStackTrace();}
      }
    }

# Async Service
To demonstrate the Promises, this project contains a simplistic immplementation of
an Async service. The async service uses the concept of a _mediator_. A mediator sits 
between the client and the implementation. When invoked, it marks the stack that there is
a potentially asynchronous invocation in progress and it removes this marker when the method 
returns. If the actual implementation of the called method is aware of the Async service,
it calls it get a Deferred object. If it gets one, it will do the async invocation and
manage the result with the deferred. Otherwise it will invoke the call synchronously.

If the mediator sees that the implementation has not asked for a Deferred, it will
assume that the implementation was not aware of the async model and will create
its own Deferred which it will immediately  wit the result of the 
the actual invocation.
 
Using the Async service therefore consists of two phases

* Create a mediator on the service you want to invoke asynchronous
* Call the mediator insde a call to the call












