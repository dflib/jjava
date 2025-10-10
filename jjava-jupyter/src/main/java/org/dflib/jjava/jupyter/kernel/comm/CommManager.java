package org.dflib.jjava.jupyter.kernel.comm;

import com.google.gson.JsonObject;
import org.dflib.jjava.jupyter.channels.JupyterSocket;
import org.dflib.jjava.jupyter.channels.ReplyEnvironment;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.MessageContext;
import org.dflib.jjava.jupyter.messages.comm.CommCloseCommand;
import org.dflib.jjava.jupyter.messages.comm.CommMsgCommand;
import org.dflib.jjava.jupyter.messages.comm.CommOpenCommand;
import org.dflib.jjava.jupyter.messages.reply.CommInfoReply;
import org.dflib.jjava.jupyter.messages.request.CommInfoRequest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps track of a group of comms created by their registered {@link CommTarget}s.
 */
public class CommManager implements Iterable<Comm> {
    protected Map<String, CommTarget> targets;
    protected Map<String, Comm> comms;
    protected JupyterSocket iopub;
    protected MessageContext context;

    public CommManager() {
        this.targets = new HashMap<>();
        this.comms = new HashMap<>();
        this.iopub = null;
    }

    public void setIOPubChannel(JupyterSocket iopub) {
        this.iopub = iopub;
    }

    public void setMessageContext(MessageContext context) {
        this.context = context;
    }

    @Override
    public Iterator<Comm> iterator() {
        return this.comms.values().iterator();
    }

    /**
     * Lookup a comm by its unique id. If the id is unknown
     * to this manager it may return null.
     *
     * @param id the comm id
     * @return the {@link Comm} with the associated id or null if the id is unknown
     */
    public Comm getCommByID(String id) {
        return this.comms.get(id);
    }

    /**
     * Register a new comm that this manager should forward messages to in the event that
     * it receives one addressed to a comm with the {@code comm}'s id.
     *
     * @param comm the comm to register with this handler
     */
    public void registerComm(Comm comm) {
        this.comms.put(comm.getID(), comm);
    }

    /**
     * Unregister a comm from this manager. This prevents the manager from forwarding messages
     * to a previously {@link #registerComm(Comm) registered} comm with the {@code id}.
     *
     * @param id the id of the destination to unregister
     * @return the comm that was unregistered or null if nothing was unregistered.
     */
    public Comm unregisterComm(String id) {
        return this.comms.remove(id);
    }

    /**
     * Open a communication with the frontend. In the event that the front end does
     * not have a target registered with the {@code targetName} the expected behaviour is for
     * it to send a {@code comm_close} message as soon as possible but there is never any
     * confirmation that the comm is open.
     *
     * @param targetName the name of the target on the frontend to message
     * @param factory    a comm producer. This is used to create the comm.
     * @param <T>        the type of {@link Comm} that the {@code factory} produces.
     * @return a comm who's {@link Comm#send(JsonObject) send} method is targeted at a new comm
     * create on the frontend by the target registered with the {@code targetName} or
     * {@code null} if the manager could not open the comm.
     * <p>
     * The latter may happen if the manager is not connected to the frontend
     */
    public <T extends Comm> T openComm(String targetName, CommFactory<T> factory) {
        if (this.iopub == null)
            return null;
        String id = UUID.randomUUID().toString();

        CommOpenCommand content = new CommOpenCommand(id, targetName, new JsonObject());
        Message<CommOpenCommand> message = new Message<>(this.context, CommOpenCommand.MESSAGE_TYPE, content);

        T comm = factory.produce(this, id, targetName, message);

        this.iopub.sendMessage(message);

        this.registerComm(comm);

        return comm;
    }

    /**
     * Send a message to a comm's frontend component. See {@link Comm#send(JsonObject, Map, List)} as well as
     * {@link Comm#send(JsonObject)} which is more likely the method to use as the metadata and blobs are lower level
     * constructs exposed for completeness but are often not necessary.
     * <p>
     * See {@link #messageComm(String, JsonObject)} for the higher level partner to this method.
     *
     * @param commID   the id of the target comm (or the id of the sending comm as both share the same id)
     * @param data     the data to send to the frontend
     * @param metadata any metadata to attach to the message being sent. May be {@code null} if no metadata is present.
     * @param blobs    any additional raw data to attach to the message. May be {@code null} if no blobs are present.
     */
    public void messageComm(String commID, JsonObject data, Map<String, Object> metadata, List<byte[]> blobs) {
        CommMsgCommand content = new CommMsgCommand(commID, data);
        Message<CommMsgCommand> message = new Message<>(this.context, CommMsgCommand.MESSAGE_TYPE, content, blobs, metadata);

        this.iopub.sendMessage(message);
    }

    /**
     * Send a message to a comm's frontend component. See {@link Comm#send(JsonObject)}
     *
     * @param commID the id of the target comm (or the id of the sending comm as both share the same id)
     * @param data   the data to send to the frontend
     */
    public void messageComm(String commID, JsonObject data) {
        this.messageComm(commID, data, null, null);
    }

    /**
     * Close both sides of a communication. This should be invoked whenever a comm is no longer
     * is use or destroyed as a counterpart is living in the frontend. Failing to invoke this may
     * leak comm instances on the frontend as well as possibly leaving the manager holding on to
     * dead references. See {@link Comm#close()}.
     *
     * @param comm the comm to close
     */
    public void closeComm(Comm comm) {
        CommCloseCommand content = new CommCloseCommand(comm.getID(), new JsonObject());
        Message<CommCloseCommand> message = new Message<>(this.context, CommCloseCommand.MESSAGE_TYPE, content);

        this.iopub.sendMessage(message);

        Comm unregistered = this.unregisterComm(comm.getID());
        if (unregistered != null)
            unregistered.onClose(message, true);
    }

    /**
     * Register a target for comm creation at the frontend's request. A target must
     * first be registered in the kernel so that the frontend may ask to create a new
     * comm for speaking with the target.
     *
     * @param targetName the name of the target which must be specified by frontend's
     *                   opening up the communication
     * @param target     a {@link CommTarget} responsible for creating new comms at this
     *                   target name
     */
    public void registerTarget(String targetName, CommTarget target) {
        this.targets.put(targetName, target);
    }

    /**
     * Unregister a target. This doesn't unregister comms with that target name but rather
     * prevents the target from creating anything new.
     * <p>
     * See also {@link #registerTarget(String, CommTarget)}
     *
     * @param targetName the name of the target to unregister
     */
    public void unregisterTarget(String targetName) {
        this.targets.remove(targetName);
    }

    /**
     * Lookup a target with the given name. See {@link #registerTarget(String, CommTarget)}
     *
     * @param targetName the target name to lookup
     * @return the {@link CommTarget} registered with the {@code targetName}
     */
    public CommTarget getTarget(String targetName) {
        return this.targets.get(targetName);
    }

    // Default comm message handlers. These shouldn't need to be overridden but are more like
    // lambda targets that capture this comm manager in its scope.

    public void handleCommOpenCommand(ReplyEnvironment env, Message<CommOpenCommand> commOpenCommandMessage) {
        CommOpenCommand openCommand = commOpenCommandMessage.getContent();

        env.setBusyDeferIdle();

        CommTarget target = this.getTarget(openCommand.getTargetName());
        if (target == null) {
            CommCloseCommand closeCommand = new CommCloseCommand(openCommand.getCommID(), new JsonObject());
            env.publish(closeCommand);
        } else {
            Comm comm = target.createComm(this, openCommand.getCommID(), openCommand.getTargetName(), commOpenCommandMessage);
            this.registerComm(comm);
        }
    }

    public void handleCommMsgCommand(ReplyEnvironment env, Message<CommMsgCommand> commMsgCommandMessage) {
        CommMsgCommand msgCommand = commMsgCommandMessage.getContent();

        env.setBusyDeferIdle();

        Comm comm = this.getCommByID(msgCommand.getCommID());
        if (comm != null) {
            comm.onMessage(commMsgCommandMessage);
        }
    }

    public void handleCommCloseCommand(ReplyEnvironment env, Message<CommCloseCommand> commCloseCommandMessage) {
        CommCloseCommand closeCommand = commCloseCommandMessage.getContent();

        env.setBusyDeferIdle();

        Comm comm = this.unregisterComm(closeCommand.getCommID());
        if (comm != null) {
            comm.onClose(commCloseCommandMessage, false);
        }
    }

    public void handleCommInfoRequest(ReplyEnvironment env, Message<CommInfoRequest> commInfoRequestMessage) {
        CommInfoRequest request = commInfoRequestMessage.getContent();

        env.setBusyDeferIdle();

        Map<String, CommInfoReply.CommInfo> comms = new LinkedHashMap<>();

        String targetNameFilter = request.getTargetName();
        if (targetNameFilter != null) {
            this.forEach(comm -> {
                if (targetNameFilter.equals(comm.getTargetName()))
                    comms.put(comm.getID(), new CommInfoReply.CommInfo(comm.getTargetName()));
            });
        } else {
            this.forEach(comm -> comms.put(comm.getID(), new CommInfoReply.CommInfo(comm.getTargetName())));
        }

        env.reply(new CommInfoReply(comms));
    }
}
