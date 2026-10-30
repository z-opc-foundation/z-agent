package com.zifang.z.agent.center.core.skill;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

@Component
public class SkillReconciliationScheduler {

    private static final Logger log = LogManager.getLogger(SkillReconciliationScheduler.class);

    @Resource
    private SkillDistributor distributor;

    /**
     * Run reconcile every 5 minutes. Cron: 0 0-59/5 * * * *
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void reconcile() {
        long t0 = System.currentTimeMillis();
        try {
            int count = distributor.reconcileAll();
            log.info("[scheduler] Skill reconcile done: {} instances in {}ms",
                    count, System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.error("[scheduler] Skill reconcile failed", e);
        }
    }
}
