/*
 * Copyright (c) 2011 Nat Pryce, Timo Meinen.
 *
 * This file is part of Team Piazza.
 *
 * Team Piazza is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Team Piazza is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.natpryce.piazza;

import com.natpryce.piazza.pluginConfiguration.PiazzaConfiguration;
import com.timgroup.jgravatar.Gravatar;
import com.timgroup.jgravatar.GravatarDefaultImage;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.users.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Timo Meinen
 */
public class PiazzaUserAdapter implements UserModelListener {

    private final UserModel userModel;
    private UserGroup userGroup = new UserGroup();
    private final PiazzaConfiguration piazzaConfiguration;

    public PiazzaUserAdapter(SBuildServer server, UserModel userModel, PiazzaConfiguration piazzaConfiguration) {
        this.userModel = userModel;
        this.piazzaConfiguration = piazzaConfiguration;
        reloadUsers();

        server.getUserModel().addListener(this);
    }

    @Override
    public void userAccountCreated(@NotNull User user) {
        reloadUsers();
    }

    @Override
    public void userAccountChanged(@NotNull User user) {
        reloadUsers();
    }

    @Override
    public void userAccountRemoved(@NotNull User user) {
        reloadUsers();
    }

    @Override
    public void userRolesChanged(@NotNull User user) {
        reloadUsers();
    }

    void reloadUsers() {
        this.userGroup = new UserGroup();
        Set<SUser> users = userModel.getAllUsers().getUsers();
        for (SUser user : users) {
            String portraitUrl = PiazzaNotificator.getPortraitUrl(user);
            if (StringUtils.hasText(portraitUrl)) {
                addUserToPiazzaMonitor(user, portraitUrl);
            } else {
                addUserToPiazzaMonitor(user, determineGravatarUrl(user));
            }
        }
    }

    private String determineGravatarUrl(SUser user) {
        Gravatar gravatar = new Gravatar()
                .setDefaultImage(GravatarDefaultImage.IDENTICON)
                .setSize(piazzaConfiguration.getMaxPortraitSize());

        return gravatar.getUrl(createGravatarUsernameOrEmail(user));
    }

    private String createGravatarUsernameOrEmail(SUser user) {
        if (StringUtils.hasText(user.getEmail()))
            return user.getEmail();
        else
            return user.getUsername();
    }

    private void addUserToPiazzaMonitor(SUser user, String portraitUrl) {
        Set<String> vcsNicknames = getVcsNicknames(user);
        PiazzaUser piazzaUser = new PiazzaUser(user.getDescriptiveName(), vcsNicknames, portraitUrl);
        this.userGroup.add(piazzaUser);
    }

    private Set<String> getVcsNicknames(SUser user) {
        Set<String> vcsNicknames = new HashSet<>();
        List<VcsUsernamePropertyKey> vcsUsernameProperties = user.getVcsUsernameProperties();
        for (VcsUsernamePropertyKey vcsUsernameProperty : vcsUsernameProperties) {
            String nickname = user.getPropertyValue(vcsUsernameProperty);
            if (nickname != null) {
                vcsNicknames.add(nickname);
            }
        }
        return vcsNicknames;
    }

    public UserGroup getUserGroup() {
        return userGroup;
    }
}
