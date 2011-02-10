/**
 * Copyright 2009-2010 LinkedIn, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.linkedin.norbert.network.netty

import org.specs.Specification
import org.specs.mock.Mockito
import com.linkedin.norbert.network.common.NetworkStatisticsActor
import java.util.UUID
import com.linkedin.norbert.util.MockClock
import com.linkedin.norbert.cluster.Node

class ClientStatisticsRequestStrategySpec extends Specification with Mockito {
  "ClientStatisticsRequestStrategy" should {
    "route away from misbehaving nodes" in {
      val statsActor = new NetworkStatisticsActor[Int, UUID](MockClock, 1000L)
      statsActor.start

      val strategy = new ClientStatisticsRequestStrategy(statsActor)

      val nodes = (0 until 5).map { nodeId => Node(nodeId, "foo", true) }

      // Give everybody 10 requests
      val requests = (0 until 5).map { nodeId =>
        val uuids = (0 until 10).map(i => UUID.randomUUID)
        uuids.foreach{ uuid => statsActor ! statsActor.Stats.BeginRequest(nodeId, uuid) }
        (nodeId, uuids)
      }.toMap

      (0 until 4).foreach{ nodeId =>
        MockClock.currentTime = (nodeId + 1)

        val uuids = requests(nodeId)
        uuids.zipWithIndex.foreach { case (uuid, index) =>
          statsActor ! statsActor.Stats.EndRequest(nodeId, uuid)
        }
      }

      MockClock.currentTime = 5

      nodes.foreach { node =>
        strategy.canServeRequest(node) must beTrue
      }

      MockClock.currentTime = 15
      strategy.canServeRequest(nodes(4)) must beFalse

//      requests(4).zipWithIndex.foreach { case(uuid, index) =>
//        val nodeId = 4
//        MockClock.currentTime = 2 * (nodeId + 1) * index
//        statsActor ! statsActor.Stats.EndRequest(nodeId, uuid)
//      }

//      strategy.canServeRequest(nodes(4)) must beFalse

    }
  }
}