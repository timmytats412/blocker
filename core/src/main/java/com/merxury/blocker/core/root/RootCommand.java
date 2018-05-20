package com.merxury.blocker.core.root;

import android.support.annotation.NonNull;
import android.util.Log;

import com.merxury.blocker.core.exception.ProcessUnexpectedTerminateException;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by Mercury on 2018/2/4.
 */

public class RootCommand {
    private static final String TAG = "RootCommand";

    @NonNull
    public synchronized static String runBlockingCommand(final String comm) throws RootDeniedException, IOException, TimeoutException {
        final AtomicReference<String> returnItem = new AtomicReference<>();
        final AtomicReference<Throwable> returnException = new AtomicReference<>();

        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                Command command = new Command(0, comm) {
                    @Override
                    public void commandOutput(int id, String line) {
                        //TODO get output
                        Log.d(TAG, "commandOutput");
                        Log.d(TAG, line);
                        emitter.onNext(line);
                        super.commandOutput(id, line);
                    }

                    @Override
                    public void commandTerminated(int id, String reason) {
                        String msg = "commandTerminated";
                        Log.d(TAG, msg);
                        emitter.onError(new ProcessUnexpectedTerminateException(reason));
                        super.commandTerminated(id, reason);
                    }

                    @Override
                    public void commandCompleted(int id, int exitcode) {
                        Log.d(TAG, "commandCompleted");
                        emitter.onComplete();
                        super.commandCompleted(id, exitcode);
                    }
                };
                RootTools.getShell(true).add(command);
            }
        }).blockingSubscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(String s) {
                returnItem.set(s);
            }

            @Override
            public void onError(Throwable e) {
                returnException.set(e);
            }

            @Override
            public void onComplete() {
            }
        });

        if (returnException.get() != null) {
            Throwable exception = returnException.get();
            if (exception instanceof RootDeniedException) {
                throw (RootDeniedException) exception;
            } else if (exception instanceof TimeoutException) {
                throw (TimeoutException) exception;
            } else if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new RuntimeException(exception);
            }
        }
        return returnItem.get();
    }

}
