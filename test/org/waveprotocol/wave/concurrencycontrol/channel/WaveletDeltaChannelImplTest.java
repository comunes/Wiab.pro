/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.wave.concurrencycontrol.channel;

import org.waveprotocol.wave.common.logging.PrintLogger;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.Recoverable;
import org.waveprotocol.wave.clientserver.ReturnCode;
import org.waveprotocol.wave.clientserver.ReturnStatus;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.impl.EmptyWaveletSnapshot;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import static junit.framework.Assert.assertEquals;
import junit.framework.TestCase;

/**
 * Tests the {@link WaveletDeltaChannelImpl} by mocking the back-end channel.
 *
 */

public class WaveletDeltaChannelImplTest extends TestCase {

  private static long TIMESTAMP = 12345;

  /**
   * A mock delta channel receiver which support setting expectations of method
   * calls.
   */
  private static class MockReceiver implements WaveletDeltaChannel.Receiver {
    private static enum Method { CONNECT, ACK, COMMIT, DELTA, NACK }

    private final Queue<Object[]> expectations = new LinkedList<Object[]>();

    public void expectConnection(HashedVersion connectVersion, HashedVersion lastModifiedVersion, 
        HashedVersion unacknowledgedDeltaVersion, List<WaveletOperation> operations) {
      expectations.add(new Object[] {Method.CONNECT, connectVersion, lastModifiedVersion,
        unacknowledgedDeltaVersion, operations });
    }

    public void expectAck(int opsApplied, HashedVersion version) {
      expectations.add(new Object[] {Method.ACK, opsApplied, version});
    }

    public void expectCommit(long version) {
      expectations.add(new Object[] {Method.COMMIT, version});
    }

    public void expectDelta(TransformedWaveletDelta delta) {
      expectations.add(new Object[] {Method.DELTA, delta});
    }

    public void expectNack(ReturnStatus detail, long version) {
      expectations.add(new Object[] {Method.NACK, detail, version});
    }

    public void checkExpectationsSatisfied() {
      assertTrue(expectations.isEmpty());
    }

    @Override
    public void onConnection(HashedVersion connectVersion, HashedVersion lastModifiedVersion, 
        HashedVersion unacknowledgedDeltaVersion, List<WaveletOperation> operations) throws ChannelException {
      Object[] expected = expectations.remove();
      assertEquals(expected[0], Method.CONNECT);
      assertEquals("incorrect connectVersion", expected[1], connectVersion);
      assertEquals("incorrect lastModifiedVersion", expected[2], lastModifiedVersion);
      assertEquals("incorrect unacknowledgedDeltaVersion", expected[3], unacknowledgedDeltaVersion);
      assertEquals("incorrect operations", expected[4], operations);
    }

    @Override
    public void onAck(int opsApplied, HashedVersion version) {
      Object[] expected = expectations.remove();
      assertEquals(expected[0], Method.ACK);
      assertEquals("incorrect opsApplied", expected[1], opsApplied);
      assertEquals("incorrect version", expected[2], version);
    }

    @Override
    public void onCommit(long version) {
      Object[] expected = expectations.remove();
      assertEquals(expected[0], Method.COMMIT);
      assertEquals("incorrect version", expected[1], version);
    }

    @Override
    public void onDelta(TransformedWaveletDelta delta) {
      Object[] expected = expectations.remove();
      assertEquals(expected[0], Method.DELTA);
      assertEquals("incorrect delta", expected[1], delta);
    }

    @Override
    public void onNack(ReturnStatus detail, long version) throws ChannelException {
      Object[] expected = expectations.remove();
      assertTrue(detail.getCode() != ReturnCode.OK);
      assertEquals(expected[0], Method.NACK);
      assertEquals("incorrect detail", expected[1], detail);
      assertEquals("incorrect version", expected[2], version);
    }
  }

  /**
   * A mock wavelet channel which allows setting expectations for method calls.
   */
  private static class MockWaveletChannel implements WaveletChannel {
    private final Queue<WaveletDelta> expectations = new LinkedList<WaveletDelta>();
    private final Queue<SubmitCallback> awaitingAck = new LinkedList<SubmitCallback>();

    public void expectSubmit(WaveletDelta delta1) {
      expectations.add(delta1);
    }

    public void checkExpectationsSatisfied() {
      assertTrue(expectations.isEmpty());
    }

    @Override
    public void submit(WaveletDelta delta, SubmitCallback callback) {
      WaveletDelta e = expectations.remove();
      assertEquals(e, delta);
      awaitingAck.add(callback);
    }

    public void ackSubmit(int opsApplied, long version, byte[] signature) throws ChannelException {
      SubmitCallback nextCallback = awaitingAck.remove();
      nextCallback.onResponse(opsApplied, HashedVersion.of(version, signature),
          TIMESTAMP, new ReturnStatus(ReturnCode.OK));
    }

    public void nackSubmit(long version, byte[] signature, ReturnStatus reason)
        throws ChannelException {
      SubmitCallback nextCallback = awaitingAck.remove();
      nextCallback.onResponse(0, HashedVersion.of(version, signature), TIMESTAMP, reason);
    }

    public void failSubmit(ReturnStatus reason) throws ChannelException {
      SubmitCallback nextCallback = awaitingAck.remove();
      nextCallback.onResponse(0, HashedVersion.unsigned(0), TIMESTAMP, reason);
    }
  }

  private static final ParticipantId USER_ID = ParticipantId.ofUnsafe("test@example.com");
  private static final DeltaTestUtil UTIL = new DeltaTestUtil(USER_ID);
  private final static WaveId WAVE_ID = WaveId.of("example.com", "waveid");
  private final static WaveletId WAVELET_ID = WaveletId.of("example.com", "waveletid");
  private static final ObservableWaveletData.Factory<?> DATA_FACTORY =
      BasicFactories.waveletDataImplFactory();

  private MockWaveletChannel waveletChannel;
  private MockReceiver receiver;
  private final PrintLogger logger = new PrintLogger();
  private WaveletDeltaChannelImpl deltaChannel; // channel under test

  @Override
  public void setUp() {
    waveletChannel = new MockWaveletChannel();
    receiver = new MockReceiver();
    deltaChannel = new WaveletDeltaChannelImpl(waveletChannel, logger);
    deltaChannel.reset(receiver);
  }

  /**
   * Tests that resetting a new channel does nothing.
   */
  public void testResetNewChannel() {
    deltaChannel.reset(null);
    waveletChannel.checkExpectationsSatisfied();
  }

  /**
   * Tests that sending on an unconnected channel fails.
   */
  public void testSendBeforeConnectFails() {
    try {
      sendDelta(buildDelta(1, 1));
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    waveletChannel.checkExpectationsSatisfied();
  }

  /**
   * Tests that a channel with no receivers quietly drops messages.
   */
  public void testChannelWithNoReceiverDropsMessages() throws ChannelException {
    final HashedVersion initialVersion = HashedVersion.of(57, sig(1));
    final ObservableWaveletData wavelet = buildSnapshot(initialVersion);
    final TransformedWaveletDelta delta = buildServerDelta(initialVersion.getVersion(), 3);

    deltaChannel.reset(null); // Clear receiver.
    connectChannel(wavelet);
    receiveUpdateOnConnectedChannel(delta, initialVersion);
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that a last committed version sent with the connect message is
   * delivered.
   */
  public void testConnectReceivesCommittedVersion() throws ChannelException {
    final HashedVersion lastModifiedVersion = HashedVersion.of(57, sig(1));
    final HashedVersion commitVersion = HashedVersion.of(50, sig(2));

    receiver.expectConnection(lastModifiedVersion, lastModifiedVersion, null, null);
    receiver.expectCommit(commitVersion.getVersion());
    connectChannel(lastModifiedVersion, lastModifiedVersion, commitVersion, null, null);
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that the last committed version sent with a reconnect initial delta is
   * delivered.
   */
  public void testReconnectReceivesCommittedVersion() throws ChannelException {
    final HashedVersion connectVersion = HashedVersion.of(50, sig(1));
    final HashedVersion commitVersion = HashedVersion.of(55, sig(2));
    final HashedVersion lastModifiedVersion = HashedVersion.of(57, sig(3));

    final TransformedWaveletDelta delta = buildServerDelta(50, 7);

    // Expect connection.
    receiver.expectConnection(connectVersion, lastModifiedVersion, null, delta);
    receiver.expectCommit(commitVersion.getVersion());
    connectChannel(connectVersion, lastModifiedVersion, commitVersion, null, delta);
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that when the client terminates the channel the receiver
   * is not notified.
   */
  public void testClientResetTerminatesSilently() throws ChannelException {
    checkedConnectChannel(57);
    deltaChannel.reset(null);
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that a delta sent down the channel is received and the acknowledgment
   * delivered.
   */
  public void testSubmitDelta() throws ChannelException {
    final long lastModifiedVersion = 57;
    final int ops1 = 7;
    final WaveletDelta delta1 = buildDelta(lastModifiedVersion, ops1);
    final byte[] signature1 = sig(1);
    final WaveletDelta delta2 = buildDelta(lastModifiedVersion + ops1, 2);
    final int opsCommitted = 3;
    final String errorMsg2 = "SERVER_ERROR";

    checkedConnectChannel(lastModifiedVersion);

    // Send delta1.
    submitDeltaOnConnectedChannel(delta1);

    // All ops are acked.
    receiver.expectAck(ops1, HashedVersion.of(lastModifiedVersion + ops1, signature1));
    ackDeltaOnConnectedChannel(lastModifiedVersion + ops1, ops1, signature1);

    // Send delta2.
    submitDeltaOnConnectedChannel(delta2);

    // Nack with a randomly injected error, as if something just went wrong
    // server-side.
    receiver.expectNack(new ReturnStatus(ReturnCode.INTERNAL_ERROR, errorMsg2),
        lastModifiedVersion + ops1);
    nackDeltaOnConnectedChannel(lastModifiedVersion + ops1, signature1, errorMsg2,
        ReturnCode.INTERNAL_ERROR);

    // Close.
    closeChannel();
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that a delta submitted after a reset has sequence number 1.
   */
  public void testSubmitAfterResetRestartsSequence() throws ChannelException {
    final long initialVersion = 57;
    final int ops1 = 7;
    final WaveletDelta delta1 = buildDelta(initialVersion, ops1);
    final byte[] signature1 = sig(1);
    final WaveletDelta delta2 = buildDelta(initialVersion + ops1, 2);

    checkedConnectChannel(initialVersion);

    // Send delta1.
    submitDeltaOnConnectedChannel(delta1);

    // All ops are acked.
    HashedVersion version1 = HashedVersion.of(initialVersion + ops1, signature1);
    receiver.expectAck(ops1, version1);
    ackDeltaOnConnectedChannel(initialVersion + ops1, ops1, signature1);

    deltaChannel.reset(receiver);
    checkedReconnectChannel(version1, version1, version1, null, null);

    // Send delta2.
    submitDeltaOnConnectedChannel(delta2);

    // Close.
    closeChannel();
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that a delta (with commit version) received from the server is
   * delivered.
   */
  public void testReceiveDelta() throws ChannelException {
    final long initialVersion = 57;
    final byte[] commitSig = sig(1);
    final TransformedWaveletDelta delta = buildServerDelta(initialVersion, 7);
    checkedConnectChannel(initialVersion);

    // Receive and deliver delta.
    receiver.expectCommit(initialVersion);
    receiver.expectDelta(delta);
    receiveUpdateOnConnectedChannel(delta, HashedVersion.of(initialVersion, commitSig));

    // Close.
    closeChannel();
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that a last-committed-version message with no deltas is correctly
   * delivered.
   */
  public void testReceiveLastCommittedVersion() throws ChannelException {
    final long initialVersion = 57;
    final long committedVersion = 50;
    final byte[] committedSignature = sig(1);
    checkedConnectChannel(initialVersion);

    receiver.expectCommit(committedVersion);
    receiveUpdateOnConnectedChannel(null, HashedVersion.of(committedVersion, committedSignature));

    // Close.
    closeChannel();
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that a submit delta interleaves properly with received deltas when the ACK is
   * received in sequence with server deltas.
   */
  public void testSynchronizedAckDelta() throws ChannelException {
    final long initialVersion = 57;
    final int serverOps1 = 7;
    checkedConnectChannel(initialVersion);

    // Receive server delta.
    final TransformedWaveletDelta delta1 = buildServerDelta(initialVersion, serverOps1);
    receiver.expectDelta(delta1);
    receiveUpdateOnConnectedChannel(delta1);

    // Submit delta.
    final long versionAfterServer1 = initialVersion + serverOps1;
    final int clientOps = 5;
    final WaveletDelta clientDelta = buildDelta(versionAfterServer1, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Acknowledge all ops immediately.
    final long versionAfterClient = versionAfterServer1 + clientOps;
    final byte[] ackedSignature = sig(1);
    receiver.expectAck(clientOps, HashedVersion.of(versionAfterClient, ackedSignature));
    ackDeltaOnConnectedChannel(versionAfterClient, clientOps, ackedSignature);

    // Receive a second server delta.
    final int serverOps2 = 3;
    final TransformedWaveletDelta delta2 = buildServerDelta(versionAfterClient, serverOps2);
    receiver.expectDelta(delta2);
    receiveUpdateOnConnectedChannel(delta2);

    // Close.
    closeChannel();
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that a submit delta interleaves properly with received deltas when the ACK is
   * received after a subsequent server delta.
   */
  public void testLateAckDelta() throws ChannelException {
    final long initialVersion = 57;
    final int serverOps1 = 7;
    checkedConnectChannel(initialVersion);

    // Receive server delta.
    final TransformedWaveletDelta delta1 = buildServerDelta(initialVersion, serverOps1);
    receiver.expectDelta(delta1);
    receiveUpdateOnConnectedChannel(delta1);

    // Submit delta.
    final long versionAfterServer1 = initialVersion + serverOps1;
    final byte[] sigAfterServer1 = sig(1);
    final int clientOps = 5;
    final WaveletDelta clientDelta = buildDelta(versionAfterServer1, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Receive a second server delta, logically after the client ops.
    final int serverOps2 = 3;
    final long versionAfterClient = versionAfterServer1 + clientOps;
    final byte[] sigAfterClient = sig(2);
    final TransformedWaveletDelta delta2 = buildServerDelta(versionAfterClient, serverOps2);
    // Don't expect the delta yet.
    receiveUpdateOnConnectedChannel(delta2);

    // Receive commit message for a received delta while there are queued
    // messages; the message jumps the queue to be delivered immediately.
    receiver.expectCommit(versionAfterServer1);
    receiveUpdateOnConnectedChannel(null, HashedVersion.of(versionAfterServer1, sigAfterServer1));

    // Receive a commit message for the outstanding submit delta. This
    // message is delayed until the ack.
    receiveUpdateOnConnectedChannel(null, HashedVersion.of(versionAfterClient, sigAfterClient));

    // Receive the ack and expect the pending delta and commit (in the order received).
    final byte[] ackedSignature = sig(1);
    receiver.expectAck(clientOps, HashedVersion.of(versionAfterClient, ackedSignature));
    receiver.expectCommit(versionAfterClient);
    receiver.expectDelta(delta2);
    ackDeltaOnConnectedChannel(versionAfterClient, clientOps, ackedSignature);

    // Close.
    closeChannel();
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that a submit delta interleaves properly with received deltas when the ACK is
   * received before a preceding server delta.
   */
  public void testEarlyAckDelta() throws ChannelException {
    final long initialVersion = 57;
    final int serverOps1 = 7;
    checkedConnectChannel(initialVersion);

    // Submit delta.
    final int clientOps = 5;
    final WaveletDelta clientDelta = buildDelta(initialVersion, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Acknowledge the submitted delta against a future version.
    final long versionAfterServer1 = initialVersion + serverOps1;
    final long versionAfterClient = versionAfterServer1 + clientOps;
    final byte[] ackedSignature = sig(1);
    // Don't expect ack yet.
    ackDeltaOnConnectedChannel(versionAfterClient, clientOps, ackedSignature);

    // Receive the server delta and the expect pending ack.
    final TransformedWaveletDelta delta1 = buildServerDelta(initialVersion, serverOps1);
    receiver.expectDelta(delta1);
    receiver.expectAck(clientOps, HashedVersion.of(versionAfterClient, ackedSignature));
    receiveUpdateOnConnectedChannel(delta1);

    // Receive a second server delta, logically after the client ops.
    final int serverOps2 = 3;
    final long versionAfterServer2 = versionAfterClient + serverOps2;
    final TransformedWaveletDelta delta2 = buildServerDelta(versionAfterClient, serverOps2);
    receiver.expectDelta(delta2);
    receiveUpdateOnConnectedChannel(delta2);

    // Close.
    closeChannel();
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that the channel can handle receiving an ack for fewer ops than
   * were submitted, as happens when the server transforms some away.
   *
   * Only the synchronized case is tested.
   */
  public void testShrunkAckDelta() throws ChannelException {
    final long initialVersion = 57;
    final int serverOps1 = 7;
    checkedConnectChannel(initialVersion);

    // Receive server delta.
    final TransformedWaveletDelta delta1 = buildServerDelta(initialVersion, serverOps1);
    receiver.expectDelta(delta1);
    receiveUpdateOnConnectedChannel(delta1);

    // Submit delta.
    final long versionAfterServer1 = initialVersion + serverOps1;
    final int clientOps = 5;
    final WaveletDelta clientDelta = buildDelta(versionAfterServer1, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Acknowledge immediately.
    final int ackedOps = 0;
    final long versionAfterClient = versionAfterServer1 + ackedOps;
    final byte[] ackedSignature = sig(1);
    receiver.expectAck(ackedOps, HashedVersion.of(versionAfterClient, ackedSignature));
    ackDeltaOnConnectedChannel(versionAfterClient, ackedOps, ackedSignature);

    // Receive a second server delta.
    final int serverOps2 = 3;
    final TransformedWaveletDelta delta2 = buildServerDelta(versionAfterClient, serverOps2);
    receiver.expectDelta(delta2);
    receiveUpdateOnConnectedChannel(delta2);

    // Close.
    closeChannel();
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that a submit delta interleaves properly with received deltas when ops are NACKed
   * before the next server delta.
   */
  public void testSynchronizedNackDelta() throws ChannelException {
    final long initialVersion = 57;
    final int serverOps1 = 7;
    final TransformedWaveletDelta delta1 = buildServerDelta(initialVersion, serverOps1);

    checkedConnectChannel(initialVersion);

    // Receive server delta.
    receiver.expectDelta(delta1);
    receiveUpdateOnConnectedChannel(delta1);

    // Submit delta.
    final long versionAfterServer1 = initialVersion + serverOps1;
    final byte[] sigAfterServer1 = sig(1);
    final int clientOps = 5;
    final WaveletDelta clientDelta = buildDelta(versionAfterServer1, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Nack delta immediately.
    final String error = "error";
    receiver.expectNack(new ReturnStatus(ReturnCode.BAD_REQUEST, error), versionAfterServer1);
    nackDeltaOnConnectedChannel(versionAfterServer1, sigAfterServer1, error,
        ReturnCode.BAD_REQUEST);

    // Receive a second server delta.
    final int serverOps2 = 3;
    final TransformedWaveletDelta delta2 = buildServerDelta(versionAfterServer1, serverOps2);
    receiver.expectDelta(delta2);
    receiveUpdateOnConnectedChannel(delta2);

    // Try another submit.
    final long versionAfterServer2 = versionAfterServer1 + serverOps2;
    final WaveletDelta clientDelta2 = buildDelta(versionAfterServer2, clientOps);
    submitDeltaOnConnectedChannel(clientDelta2);

    // Ack.
    final long versionAfterClient2 = versionAfterServer2 + clientOps;
    final byte[] ackedSignature2 = sig(2);
    receiver.expectAck(clientOps, HashedVersion.of(versionAfterClient2, ackedSignature2));
    ackDeltaOnConnectedChannel(versionAfterClient2, clientOps, ackedSignature2);

    // Close.
    closeChannel();
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that receiving an acknowledged delta submitted by this client is
   * detected as a server-side error.
   */
  public void testReflectedSubmittedDeltaAfterAckIsError() throws ChannelException {
    final long lastModifiedVersion = 57;
    final int ops1 = 7;
    final byte[] signature1 = sig(1);
    final WaveletDelta delta1 = buildDelta(lastModifiedVersion, ops1);
    checkedConnectChannel(lastModifiedVersion);

    // Send delta1.
    submitDeltaOnConnectedChannel(delta1);

    // All ops are acked.
    receiver.expectAck(ops1, HashedVersion.of(lastModifiedVersion + ops1, signature1));
    ackDeltaOnConnectedChannel(lastModifiedVersion + ops1, ops1, signature1);

    // Receive the delta (erroneously). Expect termination to be reported.
    try {
      receiveUpdateOnConnectedChannel(buildServerDelta(lastModifiedVersion, ops1));
      fail("ChannelException expected");
    } catch (ChannelException expected) {
    }
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that receiving an ack for a delta already received by this client
   * is detected as a server-side error.
   */
  public void testAckForReceivedDeltaIsError() throws ChannelException {
    final long lastModifiedVersion = 57;
    final int ops1 = 7;
    final byte[] signature1 = sig(1);
    final WaveletDelta delta1 = buildDelta(lastModifiedVersion, ops1);
    checkedConnectChannel(lastModifiedVersion);

    // Send delta1.
    submitDeltaOnConnectedChannel(delta1);

    // Receive the delta (erroneously), but we can't detect it's an error yet.
    TransformedWaveletDelta serverDelta1 = buildServerDelta(lastModifiedVersion, ops1);
    receiver.expectDelta(serverDelta1);
    receiveUpdateOnConnectedChannel(serverDelta1);

    // Now receive the ack for the delta. Expect failure.
    try {
      ackDeltaOnConnectedChannel(lastModifiedVersion + ops1, ops1, signature1);
      fail("ChannelException expected");
    } catch (ChannelException expected) {
    }
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that an ack received for a delta submitted before a channel
   * reconnects is dropped.
   */
  public void testAckAfterReconnectIgnored() throws ChannelException {
    final long initialVersion = 57;
    final byte[] initialSignature = sig(4);
    checkedConnectChannel(initialVersion);

    // Submit delta.
    final int clientOps = 5;
    final WaveletDelta clientDelta = buildDelta(initialVersion, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Reset channel.
    deltaChannel.reset(receiver);
    checkedReconnectChannel(HashedVersion.of(0, new byte[0]), HashedVersion.of(initialVersion, initialSignature),
        HashedVersion.of(initialVersion, initialSignature), null, clientDelta);

    // Acknowledge outstanding submit.
    final long versionAfterClient = initialVersion + clientOps;
    final byte[] ackedSignature = sig(1);
    // Don't expect the ack at the receiver.
    ackDeltaOnConnectedChannel(versionAfterClient, clientOps, ackedSignature);
    receiver.checkExpectationsSatisfied();
  }

  public void testNackTooOldIsRecoverable() throws ChannelException {
    final long initialVersion = 0;
    checkedConnectChannel(initialVersion);

    // Submit delta.
    final long submitVersion = 0;
    final byte[] signature = sig(1);
    final int clientOps = 1;
    final WaveletDelta clientDelta = buildDelta(submitVersion, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Nack delta immediately with TOO_OLD.
    try {
      nackDeltaOnConnectedChannel(submitVersion, signature, "too old", ReturnCode.TOO_OLD);
      fail("Expected an exception");
    } catch (ChannelException e) {
      assertEquals(Recoverable.RECOVERABLE, e.getRecoverable());
    }
  }

  /**
   * Tests that the delta channel detects a gap in the op stream and
   * throws an exception.
   */
  public void testMissingDeltaKillsChannel() throws ChannelException {
    final long initialVersion = 57;
    final byte[] signature = sig(1);
    final int ops = 7;
    final TransformedWaveletDelta delta1 = buildServerDelta(initialVersion, ops);
    checkedConnectChannel(initialVersion);

    // Receive and deliver delta.
    receiver.expectCommit(initialVersion);
    receiver.expectDelta(delta1);
    receiveUpdateOnConnectedChannel(delta1, HashedVersion.of(initialVersion, signature));

    // Receive delta with a version number too high.
    final TransformedWaveletDelta delta2 = buildServerDelta(initialVersion + ops + 1, 1);
    try {
      receiveUpdateOnConnectedChannel(delta2, HashedVersion.of(initialVersion, signature));
      fail("Expected a ChannelException");
    } catch (ChannelException expected) {
    }

    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that the delta channel detects a gap in the op stream even when
   * there is an outstanding submission.
   */
  public void testMissingDeltaWithLateAckKillsChannel() throws ChannelException {
    final long initialVersion = 57;
    final int serverOps1 = 7;
    checkedConnectChannel(initialVersion);

    // Receive server delta.
    final TransformedWaveletDelta delta1 = buildServerDelta(initialVersion, serverOps1);
    receiver.expectDelta(delta1);
    receiveUpdateOnConnectedChannel(delta1);

    // Submit delta.
    final long versionAfterServer1 = initialVersion + serverOps1;
    final int sigAfterServer1 = 0x11111111;
    final int clientOps = 5;
    final WaveletDelta clientDelta = buildDelta(versionAfterServer1, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Receive a second server delta that's after our submission,
    // will be queued.
    final long versionAfterClient = versionAfterServer1 + clientOps;
    final int sigAfterClient = 0x22222222;
    final int serverOps2 = 3;
    final TransformedWaveletDelta delta2 = buildServerDelta(versionAfterClient, serverOps2);
    receiveUpdateOnConnectedChannel(delta2);

    // Receive a third server delta that's skipped a version.
    final long versionAfterServer2 = versionAfterClient + serverOps2;
    final int sigAfterServer2 = 0x44444444;
    final TransformedWaveletDelta delta3 = buildServerDelta(versionAfterServer2 + 1, 1);

    try {
      receiveUpdateOnConnectedChannel(delta3);
      fail("Expected a ChannelException");
    } catch (ChannelException expected) {
    }

    receiver.checkExpectationsSatisfied();
  }


  /**
   * Tests that the delta channel detects a gap in the op stream even when there
   * is an oustanding delta submission if the ack couldn't possibly account for
   * the version gap.
   */
  public void testMissingDeltaBeyondLateAckKillsChannel() throws ChannelException {
    final long initialVersion = 57;
    final int serverOps1 = 7;
    checkedConnectChannel(initialVersion);

    // Receive server delta.
    final TransformedWaveletDelta delta1 = buildServerDelta(initialVersion, serverOps1);
    receiver.expectDelta(delta1);
    receiveUpdateOnConnectedChannel(delta1);

    // Submit delta.
    final long versionAfterServer1 = initialVersion + serverOps1;
    final int sigAfterServer1 = 0x11111111;
    final int clientOps = 5;
    final WaveletDelta clientDelta = buildDelta(versionAfterServer1, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Receive a second server delta, too far ahead of the client ops.
    final long versionAfterClient = versionAfterServer1 + clientOps;
    final int sigAfterClient = 0x22222222;
    final TransformedWaveletDelta delta2 = buildServerDelta(versionAfterClient + 1, 1);

    try {
      receiveUpdateOnConnectedChannel(delta2);
      fail("Expected a ChannelException");
    } catch (ChannelException expected) {
    }

    receiver.checkExpectationsSatisfied();
  }

  /**
   * Tests that the delta channel detects a gap in the op stream when a received
   * ack doesn't account for the version gap.
   */
  public void testMissingDeltaBeyondShortAckKillsChannel() throws ChannelException {
    final long initialVersion = 57;
    final int serverOps1 = 7;
    checkedConnectChannel(initialVersion);

    // Receive server delta.
    final TransformedWaveletDelta delta1 = buildServerDelta(initialVersion, serverOps1);
    receiver.expectDelta(delta1);
    receiveUpdateOnConnectedChannel(delta1);

    // Submit delta.
    final long versionAfterServer1 = initialVersion + serverOps1;
    final int sigAfterServer1 = 0x11111111;
    final int clientOps = 5;
    final WaveletDelta clientDelta = buildDelta(versionAfterServer1, clientOps);
    submitDeltaOnConnectedChannel(clientDelta);

    // Receive a second server delta that initially looks ok.
    final long versionAfterClient = versionAfterServer1 + clientOps;
    final int sigAfterClient = 0x22222222;
    final TransformedWaveletDelta delta2 = buildServerDelta(versionAfterClient, 1);
    receiveUpdateOnConnectedChannel(delta2);

    // Receive ack for fewer than the number of ops than we
    // sent (some were transformed away).
    final byte[] ackedSignature = sig(3);
    receiver.expectAck(clientOps - 1, HashedVersion.of(versionAfterClient - 1, ackedSignature));
    try {
      ackDeltaOnConnectedChannel(versionAfterClient - 1, clientOps - 1, ackedSignature);
      fail("Expected a ChannelException");
    } catch (ChannelException expected) {
    }

    receiver.checkExpectationsSatisfied();
  }

  // --- Helper methods --- //

  /**
   * Connects the channel by sending an initial message with a wavelet snapshot.
   * The committed version is synthesized to match the snapshot version.
   *
   * @param wave initial wavelet snapshot
   */
  private void connectChannel(ObservableWaveletData wave) throws ChannelException {
    connectChannel(wave.getHashedVersion(), wave.getHashedVersion(), wave.getHashedVersion(), null, null);
  }

  private void connectChannel(HashedVersion connectVersion, HashedVersion lastModifiedVersion, HashedVersion commitVersion,
      HashedVersion unacknowledgedDeltaVersion, List<WaveletOperation> operations) throws ChannelException {
    deltaChannel.onConnection(connectVersion, lastModifiedVersion, commitVersion, unacknowledgedDeltaVersion, operations);
  }

  /**
   * Connects the channel and checks expectations.
   *
   * @param lastModifiedVersion version at which to connect.
   */
  private void checkedConnectChannel(long lastModifiedVersion) throws ChannelException {
    final HashedVersion signedVersion = HashedVersion.of(lastModifiedVersion, sig(lastModifiedVersion));
    final ObservableWaveletData wavelet = buildSnapshot(signedVersion);

    receiver.expectConnection(signedVersion, signedVersion, null, null);
    receiver.expectCommit(lastModifiedVersion);
    connectChannel(wavelet);
    receiver.checkExpectationsSatisfied();
  }

  /**
   * Reconnects the channel and checks expectations.
   */
  private void checkedReconnectChannel(HashedVersion connectVersion, HashedVersion lastModifiedVersion,
      HashedVersion commitVersion, HashedVersion unacknowlwdgedDeltaVersion, 
      List<WaveletOperation> operations) throws ChannelException {
    receiver.expectConnection(connectVersion, lastModifiedVersion, unacknowlwdgedDeltaVersion, operations);
    receiver.expectCommit(commitVersion.getVersion());
    connectChannel(connectVersion, lastModifiedVersion, commitVersion, unacknowlwdgedDeltaVersion, operations);
  }

  /**
   * Simulates channel termination.
   */
  private void closeChannel() {
    deltaChannel.reset(null);
  }

  /** Tracks state for sendDeltaOnConnectedChannel. */
  private String sendState = "uninitialized";

  /**
   * Sends a delta on a connected channel.
   * @param delta1 the delta to send
   */
  private void submitDeltaOnConnectedChannel(final WaveletDelta delta1) {
    waveletChannel.expectSubmit(delta1);
    sendState = "initial";
    sendDelta(delta1);
    assertEquals("sending", sendState);
  }

  /**
   * Sends a delta on a channel. The sendState is set to "sending" when
   * the channel takes the delta.
   */
  private void sendDelta(final WaveletDelta delta) {
    deltaChannel.send(
        new WaveletDeltaChannel.Transmitter() {

          @Override
          public ClientMessage takeMessage() {
            assertEquals("initial", sendState);
            sendState = "sending";
            return new ClientMessage(delta, false);
          }
        });
  }

  /**
   * Acknowledges a delta on a connected channel.
   *
   * @param ackedVersion version to acknowledge
   * @param opsApplied number of ops applied
   * @param signature acknowledged signature
   */
  private void ackDeltaOnConnectedChannel(long ackedVersion, int opsApplied, byte[] signature)
      throws ChannelException {
    waveletChannel.ackSubmit(opsApplied, ackedVersion, signature);
  }

  /**
   * Negatively acknowledges a delta on a connected channel (possibly acknowledging some ops).
   */
  private void nackDeltaOnConnectedChannel(long ackedVersion, byte[] signature, String reason,
      ReturnCode error) throws ChannelException {
    waveletChannel.nackSubmit(ackedVersion, signature, new ReturnStatus(error, reason));
  }

  /**
   * Receives a delta message on a connected channel.
   *
   * @param delta1 delta to receive, or null for no delta
   */
  private void receiveUpdateOnConnectedChannel(TransformedWaveletDelta delta1)
      throws ChannelException {
    receiveUpdateOnConnectedChannel(delta1, null);
  }

  /**
   * Receives a delta message on a connected channel.
   *
   * @param delta delta to receive, or null for no delta
   * @param committed last committed version, or -1 to omit
   */
  private void receiveUpdateOnConnectedChannel(TransformedWaveletDelta delta,
      HashedVersion committedVersion) throws ChannelException {
    List<TransformedWaveletDelta> deltas = CollectionUtils.newArrayList();
    if (delta != null) {
      deltas.add(delta);
    }
    deltaChannel.onWaveletUpdate(deltas, committedVersion);
  }

  /**
   * Builds a minimal snapshot message.
   *
   */
  private ObservableWaveletData buildSnapshot(HashedVersion version) {
    return DATA_FACTORY.create(
        new EmptyWaveletSnapshot(WAVE_ID, WAVELET_ID,
            new ParticipantId("creator@gwave.com"), version, 0L));
  }

  /** Builds a client delta with numOps ops. */
  private WaveletDelta buildDelta(long targetVersion, int numOps) {
    return UTIL.makeDelta(HashedVersion.unsigned(targetVersion), 123457890L, numOps);
  }

  /** Builds a server delta with numOps ops. */
  private TransformedWaveletDelta buildServerDelta(long initialVersion, int numOps) {
    return UTIL.makeTransformedDelta(1234567890L, HashedVersion.unsigned(initialVersion + numOps),
        numOps);
  }

  private static byte[] sig(long v) {
    return new byte[] {( byte)v, (byte)v, (byte)v, (byte)v };
  }
}
