package in.projecteka.dataflow;

import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

import java.util.Map;
import java.util.stream.Collectors;

public class MdcContextLifter<T> implements CoreSubscriber<T> {

    CoreSubscriber<T> coreSubscriber;

    public MdcContextLifter(CoreSubscriber<T> coreSubscriber) {
        this.coreSubscriber = coreSubscriber;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        copyToMdc(coreSubscriber.currentContext());
        coreSubscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(T obj) {
        copyToMdc(coreSubscriber.currentContext());
        coreSubscriber.onNext(obj);
    }

    @Override
    public void onError(Throwable t) {
        copyToMdc(coreSubscriber.currentContext());
        coreSubscriber.onError(t);
    }

    @Override
    public void onComplete() {
        copyToMdc(coreSubscriber.currentContext());
        coreSubscriber.onComplete();
    }

    @Override
    public Context currentContext() {
        return coreSubscriber.currentContext();
    }

    private void copyToMdc(Context context) {
        if (!context.isEmpty()) {
            Map<String, String> map = context.stream()
                    .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
            MDC.setContextMap(map);
        } else {
            MDC.clear();
        }
    }
}

