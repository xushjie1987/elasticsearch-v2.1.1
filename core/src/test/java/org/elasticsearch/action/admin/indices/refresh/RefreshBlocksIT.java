/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.admin.indices.refresh;


import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.junit.Test;

import java.util.Arrays;

import static org.elasticsearch.cluster.metadata.IndexMetaData.*;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertBlocked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;
import static org.hamcrest.Matchers.equalTo;

@ClusterScope(scope = ESIntegTestCase.Scope.TEST)
public class RefreshBlocksIT extends ESIntegTestCase {

    @Test
    public void testRefreshWithBlocks() {
        createIndex("test");
        ensureGreen("test");

        NumShards numShards = getNumShards("test");

        // Request is not blocked
        for (String blockSetting : Arrays.asList(SETTING_BLOCKS_READ, SETTING_BLOCKS_WRITE)) {
            try {
                enableIndexBlock("test", blockSetting);
                RefreshResponse response = client().admin().indices().prepareRefresh("test").execute().actionGet();
                assertNoFailures(response);
                assertThat(response.getSuccessfulShards(), equalTo(numShards.totalNumShards));
            } finally {
                disableIndexBlock("test", blockSetting);
            }
        }

        // Request is blocked
        for (String blockSetting : Arrays.asList(SETTING_READ_ONLY, SETTING_BLOCKS_METADATA)) {
            try {
                enableIndexBlock("test", blockSetting);
                assertBlocked(client().admin().indices().prepareRefresh("test").get());
            } finally {
                disableIndexBlock("test", blockSetting);
            }
        }

        // Refreshing all indices is blocked when the cluster is read-only
        try {
            RefreshResponse response = client().admin().indices().prepareRefresh().execute().actionGet();
            assertNoFailures(response);
            assertThat(response.getSuccessfulShards(), equalTo(numShards.totalNumShards));

            setClusterReadOnly(true);
            assertBlocked(client().admin().indices().prepareRefresh().get());
        } finally {
            setClusterReadOnly(false);
        }
    }
}
