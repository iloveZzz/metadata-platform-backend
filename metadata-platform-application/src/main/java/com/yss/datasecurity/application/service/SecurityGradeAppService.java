package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.dto.SecurityGradeCreateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeUpdateDTO;
import com.yss.datasecurity.application.dto.SecurityGradeVO;

import java.util.List;

public interface SecurityGradeAppService {
    List<SecurityGradeVO> listAll();
    SecurityGradeVO getDetail(Long id);
    Long create(SecurityGradeCreateDTO dto);
    void update(Long id, SecurityGradeUpdateDTO dto);
    void delete(Long id);
}
