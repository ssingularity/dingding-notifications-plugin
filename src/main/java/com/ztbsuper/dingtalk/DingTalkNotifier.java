package com.ztbsuper.dingtalk;

import com.cloudbees.workflow.rest.external.RunExt;
import com.cloudbees.workflow.rest.external.StageNodeExt;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.taobao.api.ApiException;
import com.ztbsuper.Messages;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author uyangjie
 */
public class DingTalkNotifier extends Notifier implements SimpleBuildStep {

    private String accessToken;
    private String jenkinsUrl;

    private String build_success_img = "https://i.imgur.com/3dzxRZg.png";
    private String build_fail_img = "https://i.imgur.com/HM20MEe.png";
    private String stage_success_img = "https://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/sign-check-icon.png";
    private String stage_fail_img = "https://i.imgur.com/jA2Eu6u.png";
    private String stage_skip_img = "https://i.imgur.com/Uitk8Sb.jpg";
    private String stage_abort_img = "https://i.imgur.com/CFx6q0P.png";
    private String stage_unstable_img = "https://i.imgur.com/SQTOB6G.png";

    @DataBoundConstructor
    public DingTalkNotifier(String accessToken, String jenkinsUrl) {
        this.accessToken = accessToken;
        this.jenkinsUrl = jenkinsUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @DataBoundSetter
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    @DataBoundSetter
    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        jenkinsUrl = jenkinsUrl.endsWith("/") ? jenkinsUrl : jenkinsUrl + "/";
        List<OapiRobotSendRequest.Links> links = new ArrayList<>();
        links.add(generateJobLink(run));
        if (run instanceof WorkflowRun) {
            RunExt runExt = RunExt.create((WorkflowRun) run);
            runExt.getStages().forEach(x -> {
                if (!x.getName().contains("Post")){
                    links.add(generateStageLink(x, run.getUrl()));
                }
            });
        }
        OapiRobotSendRequest.Feedcard feedcard = new OapiRobotSendRequest.Feedcard();
        feedcard.setLinks(links);
        sendFeedcardMessage(feedcard);
    }

    public void sendFeedcardMessage(OapiRobotSendRequest.Feedcard feedcard) {
        String robotUrl = "https://oapi.dingtalk.com/robot/send?access_token=" + accessToken;
        DingTalkClient client = new DefaultDingTalkClient(robotUrl);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype("feedCard");
        request.setFeedCard(feedcard);
        try {
            client.execute(request);
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    private OapiRobotSendRequest.Links generateStageLink(StageNodeExt stage, String url) {
        StringBuilder sb = new StringBuilder();
        String stageName = Util.generateHelixStageName(stage.getName());
        sb.append("Stage:\t\t" + stageName + "\t\t");
        sb.append("\n");
        sb.append("Duration:\t" + Util.convertMs2HourType(stage.getDurationMillis()));
        String title = sb.toString();
        String messageUrl = jenkinsUrl + url;
        switch (stage.getStatus()) {
            case FAILED:
                return generateLink(title, messageUrl, stage_fail_img);
            case ABORTED:
                return generateLink(title, messageUrl, stage_abort_img);
            case SUCCESS:
                return generateLink(title, messageUrl, stage_success_img);
            default:
                return generateLink(title, messageUrl, stage_unstable_img);
        }
    }

    private OapiRobotSendRequest.Links generateJobLink(Run run) {
        String title = run.getFullDisplayName() + " Result: " + run.getResult();
        String messageUrl = jenkinsUrl + run.getUrl();
        if (run.getResult().equals(Result.SUCCESS)) {
            return generateLink(title, messageUrl, build_success_img);
        }
        else {
            return generateLink(title, messageUrl, build_fail_img);
        }
    }

    private OapiRobotSendRequest.Links generateLink(String title, String messageUrl, String picUrl) {
        OapiRobotSendRequest.Links link = new OapiRobotSendRequest.Links();
        link.setTitle(title);
        link.setMessageURL(messageUrl);
        link.setPicURL(picUrl);
        return link;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Symbol("pipiDingTalk")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheck(@QueryParameter String accessToken, @QueryParameter String notifyPeople) {
            if (StringUtils.isBlank(accessToken)) {
                return FormValidation.error(Messages.DingTalkNotifier_DescriptorImpl_AccessTokenIsNecessary());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.DingTalkNotifier_DescriptorImpl_DisplayName();
        }
    }
}
