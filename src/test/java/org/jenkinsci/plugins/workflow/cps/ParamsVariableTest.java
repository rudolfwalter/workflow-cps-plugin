/*
 * The MIT License
 *
 * Copyright 2016 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.workflow.cps;

import hudson.model.BooleanParameterDefinition;
import hudson.model.BooleanParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PasswordParameterDefinition;
import hudson.model.PasswordParameterValue;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import javax.inject.Inject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class ParamsVariableTest {

    @Rule public JenkinsRule r = new JenkinsRule();

    @Inject private GlobalVariableSet.GlobalVariableProvider globalVariables;

    @Before public void setup() {
        r.jenkins.getInjector().injectMembers(this);
    }

    @Issue("JENKINS-27295")
    @Test public void smokes() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("echo(/TEXT=${params.TEXT} FLAG=${params.FLAG ? 'yes' : 'no'} PASS=${params.PASS}/)",true));
        p.addProperty(new ParametersDefinitionProperty(
            new StringParameterDefinition("TEXT", ""),
            new BooleanParameterDefinition("FLAG", false, null),
            new PasswordParameterDefinition("PASS", "", null)));
        WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0, new ParametersAction(
            new StringParameterValue("TEXT", "hello"),
            new BooleanParameterValue("FLAG", true),
            new PasswordParameterValue("PASS", "s3cr3t"))));
        r.assertLogContains("TEXT=hello", b);
        r.assertLogContains("FLAG=yes", b);
        r.assertLogContains("PASS=s3cr3t", b);
    }

    @Issue("JENKINS-42367")
    @Test public void nullValue() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("echo(/TEXT=${params.TEXT}/)",true));
        p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("TEXT", "")));
        r.assertLogContains("TEXT=null", r.assertBuildStatusSuccess(p.scheduleBuild2(0, new ParametersAction(new StringParameterValue("TEXT", /* not possible via UI, but to simulate other ParameterValue impls */null)))));
    }

    @Issue("JENKINS-55091")
    @Test public void resultType() throws Exception {
        for (GlobalVariable globalVar : globalVariables.forJob(null)) {
            if (globalVar.getName().equals("params")) {
                Type type = globalVar.getType();
                Assert.assertTrue(type instanceof ParameterizedType);
                Assert.assertEquals(((ParameterizedType) type).getRawType(), Map.class);
                Assert.assertArrayEquals(((ParameterizedType)type).getActualTypeArguments(), new Type[] { String.class, Object.class });
                break;
            }
        }
    }
}
