/*
 * Copyright 2014 Orient Technologies.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orientechnologies.lucene;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.lucene.engine.OLuceneIndexEngineDelegate;
import com.orientechnologies.lucene.index.OLuceneFullTextIndex;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseInternal;
import com.orientechnologies.orient.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.orient.core.exception.OConfigurationException;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexEngine;
import com.orientechnologies.orient.core.index.OIndexFactory;
import com.orientechnologies.orient.core.index.OIndexInternal;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.storage.impl.local.OAbstractPaginatedStorage;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OLuceneIndexFactory implements OIndexFactory, ODatabaseLifecycleListener {

  public static final String       LUCENE_ALGORITHM = "LUCENE";

  private static final Set<String> TYPES;
  private static final Set<String> ALGORITHMS;

  static {
    final Set<String> types = new HashSet<String>();
    types.add(OClass.INDEX_TYPE.FULLTEXT.toString());
    TYPES = Collections.unmodifiableSet(types);
  }

  static {
    final Set<String> algorithms = new HashSet<String>();
    algorithms.add(LUCENE_ALGORITHM);
    ALGORITHMS = Collections.unmodifiableSet(algorithms);
  }

  public OLuceneIndexFactory() {
    this(false);
  }

  public OLuceneIndexFactory(boolean manual) {
    if (!manual)
      Orient.instance().addDbLifecycleListener(this);

  }

  @Override
  public int getLastVersion() {
    return 0;
  }

  @Override
  public Set<String> getTypes() {
    return TYPES;
  }

  @Override
  public Set<String> getAlgorithms() {
    return ALGORITHMS;
  }

  @Override
  public OIndexInternal<?> createIndex(String name, ODatabaseDocumentInternal database, String indexType, String algorithm,
      String valueContainerAlgorithm, ODocument metadata, int version) throws OConfigurationException {

    OAbstractPaginatedStorage storage = (OAbstractPaginatedStorage) database.getStorage().getUnderlying();

    if (metadata == null)
      metadata = new ODocument().field("analyzer", StandardAnalyzer.class.getName());

    if (OClass.INDEX_TYPE.FULLTEXT.toString().equals(indexType)) {
      return new OLuceneFullTextIndex(name, indexType, LUCENE_ALGORITHM, version, storage, valueContainerAlgorithm, metadata);
    }

    throw new OConfigurationException("Unsupported type : " + algorithm);
  }

  @Override
  public OIndexEngine createIndexEngine(String algorithm, String name, Boolean durableInNonTxMode, OStorage storage, int version,
      Map<String, String> engineProperties) {

    return new OLuceneIndexEngineDelegate(name, durableInNonTxMode, storage, version);

  }

  @Override
  public PRIORITY getPriority() {
    return PRIORITY.REGULAR;
  }

  @Override
  public void onCreate(ODatabaseInternal iDatabase) {
    OLogManager.instance().debug(this, "onCreate");

  }

  @Override
  public void onOpen(ODatabaseInternal iDatabase) {
    OLogManager.instance().debug(this, "onOpen");

  }

  @Override
  public void onClose(ODatabaseInternal iDatabase) {
    OLogManager.instance().debug(this, "onClose");
  }

  @Override
  public void onDrop(final ODatabaseInternal iDatabase) {
    try {
      if (iDatabase.isClosed())
        return;

      OLogManager.instance().debug(this, "Dropping Lucene indexes...");
      for (OIndex idx : iDatabase.getMetadata().getIndexManager().getIndexes()) {

        if (idx.getInternal() instanceof OLuceneFullTextIndex) {

          OLogManager.instance().debug(this, "- index '%s'", idx.getName());
          idx.delete();
        }
      }

    } catch (Exception e) {
      OLogManager.instance().warn(this, "Error on dropping Lucene indexes", e);
    }
  }

  @Override
  public void onCreateClass(ODatabaseInternal iDatabase, OClass iClass) {
  }

  @Override
  public void onDropClass(ODatabaseInternal iDatabase, OClass iClass) {
  }

  @Override
  public void onLocalNodeConfigurationRequest(ODocument iConfiguration) {
  }
}
