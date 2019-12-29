package com.ananops.provider.service.hystrix;

import com.ananops.base.dto.LoginAuthDto;
import com.ananops.provider.model.dto.PmcProjectDto;
import com.ananops.provider.service.PmcProjectFeignApi;
import com.ananops.wrapper.Wrapper;
import org.springframework.stereotype.Component;

/**
 * Created By ChengHao On 2019/12/20
 */
@Component
public class PmcProjectFeignHystrix implements PmcProjectFeignApi {

    @Override
    public Wrapper saveProject(PmcProjectDto pmcProjectDto) {
        return null;
    }
}
