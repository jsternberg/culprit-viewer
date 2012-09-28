package jenkins.model.plugins;

import com.google.common.collect.ImmutableSortedSet;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.TransientBuildActionFactory;
import hudson.model.User;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.lang.reflect.Field;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;

public class BuildCulprits implements Action {

    private transient AbstractBuild<?,?> build;

    public BuildCulprits(AbstractBuild<?,?> build) {
        this.build = build;
    }

    public String getIconFileName() {
        return "user.png";
    }

    public String getDisplayName() {
        return "Build Culprits";
    }

    public String getUrlName() {
        return "buildCulprits";
    }

    public AbstractBuild getBuild() {
        return build;
    }

    public Set<User> getCulprits() {
        return build.getCulprits();
    }

    public void doRefresh(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        try {
            Field culpritsField = AbstractBuild.class.getDeclaredField("culprits");
            culpritsField.setAccessible(true);
            culpritsField.set(build, null);

            HashSet<String> r = new HashSet<String>();
            for (User u : build.getCulprits())
                r.add(u.getId());
            culpritsField.set(build, ImmutableSortedSet.copyOf(r));
            build.save();
        } catch (NoSuchFieldException e) {
            // The private interface changed from under us... whoops
        } catch (IllegalAccessException e) {
            // Should not hit this since we've made the attribute accessible
        }
        rsp.sendRedirect(".");
    }

    @Extension
    public static final class DescriptorImpl extends TransientBuildActionFactory {
        public Collection<? extends Action> createFor(AbstractBuild build) {
            Result r = build.getResult();
            if (r != null && r.isWorseThan(Result.SUCCESS))
                return Collections.singletonList(new BuildCulprits(build));
            else
                return Collections.emptyList();
        }
    }
}
