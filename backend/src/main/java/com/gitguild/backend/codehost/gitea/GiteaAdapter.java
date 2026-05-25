package com.gitguild.backend.codehost.gitea;

import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.gitea.dto.RepositoryInfo;
import java.util.List;

public interface GiteaAdapter {

    RepositoryInfo getRepository(String owner, String repo);

    List<IssueInfo> listIssues(String owner, String repo);

    PrInfo getPullRequest(String owner, String repo, int prNumber);

    List<BranchInfo> listBranches(String owner, String repo);
}
