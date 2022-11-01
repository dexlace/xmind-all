package com.dexlace.boot.pro;

import org.springframework.beans.factory.Aware;

public interface MyAware extends Aware {

    void setFlag(Flag flag);

}
