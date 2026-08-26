package com.yss.datasecurity.application.service;

import com.yss.datasecurity.application.dto.InstallPackageDTO;
import com.yss.datasecurity.application.dto.ProjectPackageVO;
import com.yss.datasecurity.application.dto.StaticAlgorithmVO;
import com.yss.datasecurity.application.dto.StaticMaskTestDTO;
import com.yss.datasecurity.application.dto.StaticMaskTestResultVO;

import java.util.List;

public interface StaticMaskingAppService {

    List<StaticAlgorithmVO> listAlgorithms(String keyword, String algorithmType);

    List<ProjectPackageVO> listProjectPackages(String keyword, String status);

    boolean installPackage(InstallPackageDTO dto);

    StaticMaskTestResultVO testAlgorithm(StaticMaskTestDTO dto);
}
