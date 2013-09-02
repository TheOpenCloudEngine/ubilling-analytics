/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.osgi.bundles.analytics.dao.factory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.joda.time.LocalDate;

import com.ning.billing.account.api.Account;
import com.ning.billing.clock.Clock;
import com.ning.billing.entitlement.api.SubscriptionEvent;
import com.ning.billing.osgi.bundles.analytics.AnalyticsRefreshException;
import com.ning.billing.osgi.bundles.analytics.dao.model.BusinessAccountTransitionModelDao;
import com.ning.billing.osgi.bundles.analytics.dao.model.BusinessModelDaoBase.ReportGroup;
import com.ning.billing.util.audit.AuditLog;
import com.ning.billing.util.callcontext.CallContext;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillDataSource;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillLogService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class BusinessAccountTransitionFactory extends BusinessFactoryBase {

    public BusinessAccountTransitionFactory(final OSGIKillbillLogService logService,
                                            final OSGIKillbillAPI osgiKillbillAPI,
                                            final OSGIKillbillDataSource osgiKillbillDataSource,
                                            final Clock clock) {
        super(logService, osgiKillbillAPI, osgiKillbillDataSource, clock);
    }

    public Collection<BusinessAccountTransitionModelDao> createBusinessAccountTransitions(final UUID accountId,
                                                                                          final CallContext context) throws AnalyticsRefreshException {
        final Account account = getAccount(accountId, context);

        final Collection<BusinessAccountTransitionModelDao> businessAccountTransitions = new LinkedList<BusinessAccountTransitionModelDao>();

        final List<SubscriptionEvent> blockingStatesOrdered = getBlockingHistory(accountId, context);
        if (blockingStatesOrdered.size() == 0) {
            return businessAccountTransitions;
        }

        final Long accountRecordId = getAccountRecordId(account.getId(), context);
        final Long tenantRecordId = getTenantRecordId(context);
        final ReportGroup reportGroup = getReportGroup(account.getId(), context);

        final List<SubscriptionEvent> blockingStates = Lists.reverse(ImmutableList.<SubscriptionEvent>copyOf(blockingStatesOrdered));
        LocalDate previousStartDate = null;
        for (final SubscriptionEvent state : blockingStates) {
            final Long blockingStateRecordId = getBlockingStateRecordId(state.getId(), context);
            final AuditLog creationAuditLog = getBlockingStateCreationAuditLog(state.getId(), context);
            final BusinessAccountTransitionModelDao accountTransition = new BusinessAccountTransitionModelDao(account,
                                                                                                              accountRecordId,
                                                                                                              state.getServiceName(),
                                                                                                              state.getServiceStateName(),
                                                                                                              state.getEffectiveDate(),
                                                                                                              blockingStateRecordId,
                                                                                                              previousStartDate,
                                                                                                              creationAuditLog,
                                                                                                              tenantRecordId,
                                                                                                              reportGroup);
            businessAccountTransitions.add(accountTransition);
            previousStartDate = state.getEffectiveDate();
        }

        return businessAccountTransitions;
    }
}