package com.hypermax.provision;

import com.hypermax.provision.IAnimCallback;

interface IProvisionAnim {

    boolean isAnimEnd();

    void playBackAnim(int animY);
    void playNextAnim(int i);

    void registerRemoteCallback(IAnimCallback callback);
    void unregisterRemoteCallback(IAnimCallback callback);
}
