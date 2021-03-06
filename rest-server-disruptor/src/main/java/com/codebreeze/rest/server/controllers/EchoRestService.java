package com.codebreeze.rest.server.controllers;

import com.codebreeze.rest.server.ringbuffer.EchoEvent;
import com.codebreeze.rest.server.services.EchoService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.lmax.disruptor.RingBuffer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.Callable;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@RestController
@RequestMapping(value = "/echo")
public class EchoRestService {

    @Autowired
    private ListeningExecutorService listeningExecutorService;

    @Autowired
    private EchoService echoService;

    @Autowired
    private RingBuffer<EchoEvent> echoEventRingBuffer;

    @RequestMapping(
            value = "/once",
            method = RequestMethod.GET
    )
    public String addConversation(@RequestParam("text") final String text) {
        return echoService.echo(text);
    }

    @RequestMapping(
            value = "/oncec",
            method = RequestMethod.GET
    )
    public Callable<String> addConversationCallable(@RequestParam("text") final String text) {
        return () -> echoService.echo(text);
    }

    @RequestMapping(
            value = "/onced",
            method = RequestMethod.GET
    )
    public DeferredResult<String> addConversationDefferred(@RequestParam("text") final String text) {
        final DeferredResult<String> deferredResult = new DeferredResult<>();
        ListenableFuture<String> resultListenableFuture = listeningExecutorService.submit(() -> echoService.echo(text));
        Futures.addCallback(resultListenableFuture, new FutureCallback<String>() {
            public void onSuccess(String result) {
                deferredResult.setResult(result);
            }
            public void onFailure(Throwable thrown) {
                deferredResult.setErrorResult(thrown);
            }
        });
        return deferredResult;
    }

    @RequestMapping(
            value = "/oncer",
            method = RequestMethod.GET
    )
    public DeferredResult<String> addConversationDefferredR(@RequestParam("text") final String text) {
        final DeferredResult<String> deferredResult = new DeferredResult<>();
        final long seq = echoEventRingBuffer.next();

        //2 - get EchoEvent object based on it sequence.
        final EchoEvent carEvent = echoEventRingBuffer.get(seq);

        //3 - set the payload in the CarEvent.
        carEvent.setText(text);
        carEvent.setDeferredResult(deferredResult);

        //4 - publish the event using it sequence.
        echoEventRingBuffer.publish(seq);
        return deferredResult;
    }



    @RequestMapping(
            value = "/status",
            method = RequestMethod.GET
    )
    public String status() {
        return this.getClass().getSimpleName() + " is OK and Alive";
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Throwable ex) {
        return "Handled exception: " + getStackTrace(ex);
    }
}
