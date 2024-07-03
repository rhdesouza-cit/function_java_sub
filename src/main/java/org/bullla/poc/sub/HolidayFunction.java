package org.bullla.poc.sub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.gson.Gson;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HolidayFunction implements HttpFunction {

    private static final String EXTERNAL_SERVICE = System.getenv("EXTERNAL_SERVICE");
    private static final String EMULATOR_HOST = System.getenv("EMULATOR_HOST");
    private static final String PROJECT_ID = System.getenv("PROJECT_ID");
    private static final String SUBSCRIPTION_ID = System.getenv("SUBSCRIPTION_ID");

    private static Logger logger = LoggerFactory.getLogger(HolidayFunction.class);

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        HolidayClient holidayClient = FeignConfig.createClient(HolidayClient.class, EXTERNAL_SERVICE);

        // ManagedChannel channel =
        // ManagedChannelBuilder.forTarget(EMULATOR_HOST).usePlaintext().build();
        // TransportChannelProvider channelProvider = FixedTransportChannelProvider
        // .create(GrpcTransportChannel.create(channel));
        // CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        SubscriberStubSettings subscriberStubSettings = SubscriberStubSettings.newBuilder()
                // .setTransportChannelProvider(channelProvider)
                // .setCredentialsProvider(credentialsProvider)
                .build();

        try (SubscriberStub subscriber = GrpcSubscriberStub.create(subscriberStubSettings)) {
            String subscriptionName = ProjectSubscriptionName.format(PROJECT_ID, SUBSCRIPTION_ID);
            PullRequest pullRequest = PullRequest.newBuilder()
                    .setMaxMessages(1000)
                    .setSubscription(subscriptionName)
                    .setReturnImmediately(true)
                    .build();

            // Use pullCallable().futureCall to asynchronously perform this operation.
            PullResponse pullResponse = subscriber.pullCallable().call(pullRequest);

            List<String> ackIds = new ArrayList<>();
            for (ReceivedMessage message : pullResponse.getReceivedMessagesList()) {
                holidayClient
                        .postHoliday(new Gson().fromJson(message.getMessage().getData().toStringUtf8(), Holiday.class));
                ackIds.add(message.getAckId());
            }

            if (!ackIds.isEmpty()) {
                // Acknowledge received messages.
                AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
                        .setSubscription(subscriptionName)
                        .addAllAckIds(ackIds)
                        .build();
                // Use acknowledgeCallable().futureCall to asynchronously perform this
                // operation.
                subscriber.acknowledgeCallable().call(acknowledgeRequest);
            }
            logger.info("Messages received: " + pullResponse.getReceivedMessagesList());
            logger.info("Total messages: " + pullResponse.getReceivedMessagesList().size());
        }
    }

}
