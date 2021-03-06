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

package org.waveprotocol.box.webclient.search;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.http.client.Request;

import org.waveprotocol.box.webclient.search.SearchService.Callback;
import org.waveprotocol.box.webclient.search.SearchService.DigestSnapshot;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.client.wavepanel.render.ObservableDynamicRenderer;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.InboxState;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * A simple implementation of the search model, using a search service.
 * <p>
 * This search keeps a list that corresponds to the total search result size.
 * Segments of that list are filled in as necessary.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class SimpleSearch implements Search {

  private final static LoggerBundle log = new DomLogger("search");

  /**
   * Wraps a digest snapshot, but can switch to an optimistic wave-based digest
   * if a wave is available ({@link #activate} and {@link #deactivate}). The
   * snapshot this proxy wraps can also be replaced, e.g., from updated search
   * results, with {@link #update}. This proxy supports liveness, notifying
   * listeners of changes.
   */
  class DigestProxy implements Digest {
    
    /** Snapshot from the search result. Never null. */
    private DigestSnapshot staticDigest;
    
    /** Optimistic digest from a wave. May be null. */
    private WaveBasedDigest dynamicDigest;
    
    /** Listener to the dynamic digest. */
    private final WaveBasedDigest.Listener dynamicDigestListener = new WaveBasedDigest.Listener() {

      @Override
      public void onChanged() {
        // Fan out events from the live digest to this digest's listeners.
        fireOnChanged();        
      }
    };
    
    /** Dynamic renderer. */
    private ObservableDynamicRenderer dynamicRenderer;

    /** Listener to the dynamic renderer. */
    private final ObservableDynamicRenderer.Listener dynamicRendererListener =
        new ObservableDynamicRenderer.ListenerImpl() {

      @Override
      public void onBlipReady(ConversationBlip blip) {
        fireOnChanged();
      }
    };
    
    DigestProxy(DigestSnapshot staticDigest) {
      Preconditions.checkArgument(staticDigest != null);
      this.staticDigest = staticDigest;
    }

    /**
     * Destroys this object, releasing its resources.
     */
    void destroy() {
      if (dynamicDigest != null) {
        deactivate();
      }
    }

    /**
     * Switches to a live digest, sourced from a wave. This fires a change event.
     */
    void activate(WaveContext wave) {
      Preconditions.checkState(dynamicDigest == null);
      
      dynamicDigest = WaveBasedDigest.create(wave);
      dynamicDigest.addListener(dynamicDigestListener);
      
      dynamicRenderer = wave.getDynamicRenderer();
      dynamicRenderer.addListener(dynamicRendererListener);
      
      fireOnChanged();
    }

    /**
     * Abandons the live digest, falling back to a static digest. The state from
     * the live digest is pushed into a static form, so this action should not
     * cause any change to the digest state, and so does not fire a change event.
     */
    void deactivate() {
      Preconditions.checkState(dynamicDigest != null);
      staticDigest = new DigestSnapshot(getTitle(), getSnippet(), getWaveId(), getAuthor(),
          getParticipantsSnippet(), getLastModifiedTime(), getUnreadCount(), getBlipCount());
      
      dynamicRenderer.removeListener(dynamicRendererListener);
      dynamicRenderer = null;
      
      dynamicDigest.removeListener(dynamicDigestListener);
      dynamicDigest.destroy();
      dynamicDigest = null;
    }

    /**
     * Updates the static digest. Do nothing if this digest is currently live.
     */
    void update(DigestSnapshot snapshot) {
      staticDigest = snapshot;
      if (dynamicDigest == null) {
        fireOnChanged();
      }
    }

    private Digest getDelegate() {
      if (dynamicDigest != null) {
        return dynamicDigest;
      }
      Preconditions.checkArgument(staticDigest != null, "delegate is null");
      return staticDigest;
    }

    //
    // Forward Digest API to delegate.
    //

    @Override
    public WaveId getWaveId() {
      return getDelegate().getWaveId();
    }

    @Override
    public ParticipantId getAuthor() {
      return getDelegate().getAuthor();
    }

    @Override
    public List<ParticipantId> getParticipantsSnippet() {
      return getDelegate().getParticipantsSnippet();
    }

    @Override
    public String getTitle() {
      String title = getDelegate().getTitle();
      if (TitleHelper.isUnknown(title) && staticDigest != null) {
        title = staticDigest.getTitle();
      }
      return title;
    }

    @Override
    public String getSnippet() {
      String snippet = getDelegate().getSnippet();
      if (TitleHelper.isUnknown(snippet) && staticDigest != null) {
        snippet = staticDigest.getSnippet();
      }
      return snippet;
    }

    @Override
    public int getUnreadCount() {
      return getDelegate().getUnreadCount();
    }

    @Override
    public int getBlipCount() {
      return getDelegate().getBlipCount();
    }

    @Override
    public double getLastModifiedTime() {
      return getDelegate().getLastModifiedTime();
    }

    @Override
    public InboxState getInboxState() {
      return getDelegate().getInboxState();
    }

    private void fireOnChanged() {
      // TODO(hearnden): make not linear.
      fireOnDigestReady(results.indexOf(staticDigest), this);
    }
  }

  /** Service that performs searches. */
  private final SearchService searcher;

  /**
   * A list the size of the total search result, populated with digests that are
   * known to this search model.
   */
  private final List<DigestSnapshot> results = CollectionUtils.newArrayList();

  /**
   * Map of all digests.
   */
  private final StringMap<DigestProxy> digests = CollectionUtils.createStringMap();

  /** Store of all open waves in the client. */
  private final WaveStore waveStore;

  /** Listeners. */
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /** The request that is currently in flight, or {@code null}. */
  private Callback outstanding;

  /** Total size of the search result. */
  private int total = 0;

  /** Current search request was cancelled. */
  private boolean cancelled = false;

  private Request previousRequest;

  private String previousQuery;

  private String previousResponsedQuery;

  private int previousSize;

  private final WaveStore.Listener waveStoreListener = new WaveStore.Listener() {

    /**
     * If the opened wave is in the search result, switches to an optimistic digest.
     */
    @Override
    public void onOpened(WaveContext wave) {
      String id = ModernIdSerialiser.INSTANCE.serialiseWaveId(wave.getWave().getWaveId());
      DigestProxy digest = digests.get(id);
      if (digest != null) {
        log.trace().log("switching to active digest for: ", id);
        digest.activate(wave);
      }
    }

    @Override
    public void onClosed(WaveContext wave) {
      String id = ModernIdSerialiser.INSTANCE.serialiseWaveId(wave.getWave().getWaveId());
      DigestProxy digest = digests.get(id);
      if (digest != null) {
        log.trace().log("switching to passive digest for: ", id);
        digest.deactivate();
      }
    }
  };
  
  @VisibleForTesting
  SimpleSearch(SearchService searcher, WaveStore store) {
    this.searcher = searcher;
    this.waveStore = store;
  }

  /**
   * Creates a search model.
   *
   * @param searcher service that performs searches
   * @param store store of open waves
   */
  public static SimpleSearch create(SearchService searcher, WaveStore store) {
    SimpleSearch search = new SimpleSearch(searcher, store);
    search.init();
    return search;
  }

  private void init() {
    waveStore.addListener(waveStoreListener);
  }

  /**
   * Destroys this search model, releasing its resources.
   */
  public void destroy() {
    destroyDigests();
    waveStore.removeListener(waveStoreListener);
    outstanding = null;
  }

  private void destroyDigests() {
    digests.each(new ProcV<DigestProxy>() {
      
      @Override
      public void apply(String key, DigestProxy value) {
        value.destroy();
      }
    });
    digests.clear();
    results.clear();
    total = 0;
  }

  @Override
  public void find(String query, int size) {
    if (previousRequest != null && previousRequest.isPending()) {
      if (query.equals(previousQuery) && size == previousSize) {
        // Same query, we should wait to the response
        return;
      }
    }
    previousQuery = query;
    previousSize = size;
    Callback callback = new Callback() {
      
      @Override
      public void onFailure(String message) {
        if (outstanding == this && !cancelled) {
          outstanding = null;
          previousRequest = null;
          handleFailure(message);
        }
      }

      @Override
      public void onSuccess(String query, int total, List<DigestSnapshot> snapshots) {
        if (outstanding == this && !cancelled) {
          outstanding = null;
          previousRequest = null;
          handleSuccess(query, total, 0, snapshots);
        }
      }
    };

    cancelled = false;
    if (outstanding == null) {
      outstanding = callback;
      previousRequest = searcher.search(query, 0, size, callback);
      fireOnStateChanged();
    } else {
      outstanding = callback;
      previousRequest = searcher.search(query, 0, size, callback);
    }
  }

  @Override
  public void cancel() {
    cancelled = true;
  }

  /**
   * Logs an error.  Destroys the current results.
   */
  private void handleFailure(String message) {
    log.error().log("Search failed: ", message);
    destroyDigests();
    fireOnStateChanged();
  }

  /**
   * Copies the digest snapshots into this search result's state.
   */
  private void handleSuccess(String query, int total, int from, List<DigestSnapshot> newDigests) {
    if (query.equals(previousResponsedQuery)
        && this.total == total
        && from + newDigests.size() <= results.size()
        && results.subList(from, from + newDigests.size()).hashCode() == newDigests.hashCode()) {
      log.trace().log("handling vacuous update");
      // Assume no change, but notify listeners that the search is complete.
      fireOnStateChanged();
    } else {
      // For an incremental search, the result must be changed in steps that can
      // be communicated in the event language of the listener. Since the search
      // service is not incremental, computing a minimal diff is complicated.
      // Since the intent is eventually to make the search service itself
      // incremental, a brute force re-rendering here is a stop-gap.

      // Remove all digests.  Remove from last to first, so that remove is O(1).
      log.trace().log("handling changed search");
      for (int i = results.size() - 1; i >= 0; i--) {
        DigestProxy oldDigest = getDigest(i);
        results.remove(i);
        digests.remove(ModernIdSerialiser.INSTANCE.serialiseWaveId(oldDigest.getWaveId()));
        oldDigest.destroy();
      }
      // Now grow from nothing up to the new result size.
      if (this.total != total) {
        this.total = total;
      }
      ensureMinimumSize(total == Search.UNKNOWN_SIZE ? from + newDigests.size() : total);
      for (int to = from + newDigests.size(), i = from; i < to; i++) {
        results.set(i, newDigests.get(i - from));
      }
      fireOnTotalChanged(total);
      fireOnStateChanged();
    }
    previousResponsedQuery = query;
  }

  /**
   * Ensures that the result list is at least a certain size.
   */
  private void ensureMinimumSize(int total) {
    while (results.size() < total) {
      results.add(null);
    }
  }

  @Override
  public State getState() {
    return outstanding == null ? State.READY : State.SEARCHING;
  }

  @Override
  public DigestProxy getDigest(int index) {
    Preconditions.checkState(outstanding == null);
    DigestSnapshot result = results.get(index);
    WaveId waveId = result.getWaveId();
    String id = ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId);
    DigestProxy proxy = digests.get(id);
    if (proxy == null) {
      proxy = new DigestProxy(result);
      digests.put(id, proxy);

      // Switch to a live digest if the wave is open.
      WaveContext wave = waveStore.getOpenWaves().get(waveId);
      if (wave != null) {
        proxy.activate(wave);
      }
    }

    return proxy;
  }

  @Override
  public void removeDigest(WaveId waveId) {
    Preconditions.checkState(outstanding == null);
    int index = 0;
    while (index < results.size()) {
      if (results.get(index).getWaveId().equals(waveId)) {
        results.remove(index);
        index = -1;
        break;
      }
      index++;
    }
    Preconditions.checkState(index == -1);
    String waveIdString = ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId);
    DigestProxy digest = digests.get(waveIdString);
    Preconditions.checkState(digest != null);
    digests.remove(waveIdString);
    fireOnDigestRemoved(index, digest);
    fireOnTotalChanged(--total);
  }

  @Override
  public int getTotal() {
    Preconditions.checkState(outstanding == null);
    return total;
  }

  @Override
  public int getMinimumTotal() {
    return results.size();
  }

  //
  // Broadcast events.
  //

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void fireOnStateChanged() {
    for (Listener listener : listeners) {
      listener.onStateChanged();
    }
  }

  private void fireOnTotalChanged(int total) {
    for (Listener listener : listeners) {
      listener.onTotalChanged(total);
    }
  }

  private void fireOnDigestReady(int index, Digest digest) {
    for (Listener listener : listeners) {
      listener.onDigestReady(index, digest);
    }
  }

  private void fireOnDigestRemoved(int index, Digest digest) {
    for (Listener listener : listeners) {
      listener.onDigestRemoved(index, digest);
    }
  }
}
