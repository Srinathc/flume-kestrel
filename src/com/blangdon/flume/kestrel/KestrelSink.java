package com.blangdon.flume.kestrel;

import com.google.common.base.Throwables;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.KestrelCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Sink that sends events to Kestrel queue
 */
public class KestrelSink extends AbstractSink implements Configurable {

    private static final Logger logger = LoggerFactory.getLogger(KestrelSink.class);

    private String queue = null;
    private MemcachedClient client = null;
    private MemcachedClientBuilder builder = null;

    @Override
    public void start() {
        try {
            this.client = this.builder.build();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void stop() {
        try {
            this.client.shutdown();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void configure(Context context) {
        String servers = checkNotNull(context.getString("servers"),
                "List of kestrel servers not specified");
        this.queue = checkNotNull(context.getString("queue"),
                "Queue name not specified");
        this.builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(servers));
        builder.setCommandFactory(new KestrelCommandFactory());
    }

    @Override
    public Status process() throws EventDeliveryException {
        Status status = Status.READY;
        Channel channel = getChannel();
        Transaction transaction = channel.getTransaction();
        org.apache.flume.Event nextEvent = null;
        try {
            transaction.begin();
            nextEvent = channel.take();
            if (nextEvent == null) {
                status = Status.BACKOFF;
            } else {
                String message = new String(nextEvent.getBody());
                this.client.set(this.queue, 0, message);
            }
            transaction.commit();
        } catch (Exception ex) {
            transaction.rollback();
            logger.error("Failed to deliver event: " + nextEvent, ex);
            throw new EventDeliveryException("Failed to deliver event: " + nextEvent, ex);
        } finally {
            transaction.close();
        }
        return status;
    }
}
