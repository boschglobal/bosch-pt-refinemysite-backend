#!/usr/bin/env python3
import subprocess
import os
import sys
import json
import argparse
import textwrap

try:
    import requests
except ImportError:
    sys.exit("Please install missing dependency via: pip install requests or pip3 install requests")

try:
    import git
except ImportError:
    sys.exit("Please install missing dependency via: pip install gitpython or pip3 install gitpython")


class Git:
    @staticmethod
    def reset_current_master(working_directory, blacklist):
        repo_name = os.path.basename(os.path.normpath(working_directory))
        if repo_name not in blacklist:
            repo = git.Repo(working_directory)
            assert not repo.bare
            if not repo.head.is_detached and repo.is_dirty():
                print("Skipped - repo {} is dirty".format(repo_name))
                return
            print("Checkout branch master in repository: ", repo_name)
            git.Git(working_directory).execute(command=["git", "checkout", "-B", "master", "origin/master"],
                                               with_extended_output=True)

    @staticmethod
    def checkout(working_directory, blacklist, branch):
        repo_name = os.path.basename(os.path.normpath(working_directory))
        if repo_name not in blacklist:
            repo = git.Repo(working_directory)
            assert not repo.bare
            if branch in repo.branches:
                if not repo.head.is_detached and repo.is_dirty():
                    print("Skipped - repo {} is dirty".format(repo_name))
                    return

                print("Checkout branch: {} in repository: {}".format(branch, repo_name))
                git.Git(working_directory).execute(command=["git", "checkout", branch], with_extended_output=True)

    @staticmethod
    def clone(working_directory, repository, blacklist):
        if repository[0] not in blacklist and not os.path.exists(working_directory + repository[0]):
            print("Clone repository: ", repository[1])
            git.Git(working_directory).clone(repository[1])

    @staticmethod
    def delete_branch(working_directory, blacklist, branch):
        repo_name = os.path.basename(os.path.normpath(working_directory))
        if repo_name not in blacklist:
            repo = git.Repo(working_directory)
            assert not repo.bare
            if branch in repo.branches:
                if repo.head.ref.name == branch:
                    git.Git(working_directory).execute(command=["git", "checkout", "master"], with_extended_output=True)
                repo_name = os.path.basename(os.path.normpath(working_directory))
                print("Delete branch: {} in repository: {}".format(branch, repo_name))
                git.Git(working_directory).execute(command=["git", "branch", "-D", branch], with_extended_output=True)

    @staticmethod
    def fetch(working_directory, blacklist):
        if os.path.basename(os.path.normpath(working_directory)) not in blacklist \
                and Git.is_ssh_repo(working_directory):
            print("Fetch repo: ", working_directory)
            fetch_output = git.Git(working_directory).execute(command=["git", "fetch", "origin", "--prune"],
                                                              with_extended_output=True)
            for line in fetch_output[2].splitlines():
                print(line)

    @staticmethod
    def print_current_branch(working_directory, blacklist):
        if os.path.basename(os.path.normpath(working_directory)) not in blacklist \
                and Git.is_ssh_repo(working_directory):
            repo_name = os.path.basename(os.path.normpath(working_directory))
            repo = git.Repo(working_directory)
            assert not repo.bare
            repo.refs
            repo.head.ref
            print(repo_name, ":", repo.head.ref.name)

    @staticmethod
    def print_git_status(working_directory, blacklist):
        repo_name = os.path.basename(os.path.normpath(working_directory))
        if repo_name not in blacklist and Git.is_ssh_repo(working_directory):
            repo = git.Repo(working_directory)
            if repo.is_dirty():
                print("Repo {} is dirty:".format(repo_name))
                for item in repo.index.diff(None):
                    print("  ", item.a_path)

    @staticmethod
    def is_ssh_repo(working_directory):
        output, error = ProcessUtils.run("git remote get-url origin", working_directory)
        if output != '':
            if "http" not in output:
                return True
        return False


class AzureDevOps:
    @staticmethod
    def get_repositories(username, password, organization, project):
        response = requests.get("https://dev.azure.com/" + organization + "/" + project
                                + "/_apis/git/repositories?api-version=5.0-preview.1",
                                auth=(username, password))
        
        if response.status_code == 401:
            sys.exit('Azure DevOps API responded with HTTP 401 Unauthoarized. Probably your Personal Access Token in conf.json is invalid or expired.')
        if response.status_code != 200:
            sys.exit(f'Azure DevOps API responded with HTTP {response.status_code}: {response.reason}')
        
        json_response = response.json()
        return [(repository["name"], repository["sshUrl"]) for repository in iter(json_response["value"])]


class ProcessUtils:
    @staticmethod
    def run(operation, working_directory):
        p = subprocess.Popen(operation, cwd=working_directory, shell=True,
                             stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE)

        out, err = p.communicate()
        output = out.decode("utf-8").strip(' \t\n\r')
        error = err.decode("utf-8").strip(' \t\n\r')
        return output, error

    @staticmethod
    def print(output, error):
        wrapper = textwrap.TextWrapper(initial_indent="   ", subsequent_indent="   ", width=300)
        if output != '':
            print(wrapper.fill(output))
        if error != '':
            print(wrapper.fill(error))


class DirectoryUtils:
    @staticmethod
    def get_git_repositories(path):
        subdirectories = [x for x in os.listdir(path) if os.path.isdir(os.path.join(path, x))]
        return [x for x in subdirectories if os.path.exists(os.path.join(path, x, '.git'))]

    @staticmethod
    def get_path(path):
        return path if str.endswith(path, "/") else path + "/"

    @staticmethod
    def get_config():
        with open(os.path.join(sys.path[0], "conf.json"), "r") as json_data:
            return json.load(json_data)


# Check CLI parameters
parser = argparse.ArgumentParser(description='Manages your git repositories.\n\n'
                                             'Configure conf.json before running with parameters.',
                                 formatter_class=argparse.RawTextHelpFormatter)
group = parser.add_mutually_exclusive_group(required=False)

group.add_argument('--checkout', required=False, type=str,
                   help='checks out the specified branch in all repositories if exists.')
group.add_argument('--clone', required=False, action='store_true',
                   help='clones all repositories except those on blacklist.')
group.add_argument('--delete-branch', required=False, type=str,
                   help='deletes the specified branch and checks out master branch \n'
                        'in all repos if exists.')
group.add_argument('--fetch', required=False, action='store_true',
                   help='fetches all ssh repositories found in the configured directory\n'
                        'except those on blacklist.')
group.add_argument('--print-current-branch', required=False, action='store_true',
                   help='prints the current branch of all ssh repositories found in the configured directory\n'
                        'except those on blacklist.')
group.add_argument('--reset-to-master-all', required=False, action='store_true',
                   help='checks out latest version of origin/master on branch master\n'
                        'in all repos. Overwrites changes on the branch master.')
group.add_argument('--reset-to-master-except', required=False, type=str,
                   help='checks out latest version of origin/master on branch master\n'
                        'in all repos except the specified one. Overwrites changes on the\n'
                        'branch master.')
group.add_argument('--status', required=False, action='store_true',
                   help='prints git status of all ssh repositories found in the configured directory\n'
                        'except those on blacklist.')

if len(sys.argv) == 1:
    parser.print_help(sys.stderr)
    sys.exit(1)
args = vars(parser.parse_args())

# Load config json
config = DirectoryUtils.get_config()

# Execute requested operation
if args["checkout"]:
    git_repositories = DirectoryUtils.get_git_repositories(DirectoryUtils.get_path(config["directory"]))
    for repo in git_repositories:
        Git.checkout(DirectoryUtils.get_path(DirectoryUtils.get_path(config["directory"]) + repo),
                     config["blacklist"],
                     args["checkout"])

elif args["reset_to_master_all"]:
    git_repositories = DirectoryUtils.get_git_repositories(DirectoryUtils.get_path(config["directory"]))
    for repo in git_repositories:
        Git.reset_current_master(DirectoryUtils.get_path(DirectoryUtils.get_path(config["directory"]) + repo),
                                 config["blacklist"])

elif args["reset_to_master_except"]:
    git_repositories = DirectoryUtils.get_git_repositories(DirectoryUtils.get_path(config["directory"]))
    repos_to_skip = args["reset_to_master_except"].split(',')
    for repo in repos_to_skip:
        if repo not in git_repositories:
            print(args["reset_to_master_except"], "is not a git repository")
    else:
        for repo in git_repositories:
            if repo not in repos_to_skip:
                Git.reset_current_master(DirectoryUtils.get_path(
                    DirectoryUtils.get_path(config["directory"]) + repo),
                    config["blacklist"])

elif args["clone"]:
    # Read repos from azure dev ops
    azure_dev_ops = AzureDevOps()
    repos = azure_dev_ops.get_repositories(config["username"],
                                           config["token"],
                                           config["organization"],
                                           config["project"])
    # Remove outdated repositories
    repos = [repo for repo in repos if not repo[0].startswith("outdated")]
    # Clone repos
    for repo in repos:
        Git.clone(DirectoryUtils.get_path(config["directory"]), repo, config["blacklist"])

elif args["delete_branch"]:
    git_repositories = DirectoryUtils.get_git_repositories(DirectoryUtils.get_path(config["directory"]))
    git_repositories = [repo for repo in git_repositories if "outdated." not in repo]
    for repo in git_repositories:
        Git.delete_branch(DirectoryUtils.get_path(DirectoryUtils.get_path(config["directory"]) + repo),
                          config["blacklist"],
                          args["delete_branch"])

elif args["fetch"]:
    git_repositories = DirectoryUtils.get_git_repositories(DirectoryUtils.get_path(config["directory"]))
    git_repositories = [repo for repo in git_repositories if "outdated." not in repo]
    for repo in git_repositories:
        Git.fetch(DirectoryUtils.get_path(config["directory"]) + repo, config["blacklist"])

elif args["print_current_branch"]:
    git_repositories = DirectoryUtils.get_git_repositories(DirectoryUtils.get_path(config["directory"]))
    git_repositories = [repo for repo in git_repositories if "outdated." not in repo]
    for repo in git_repositories:
        Git.print_current_branch(DirectoryUtils.get_path(config["directory"]) + repo, config["blacklist"])

elif args["status"]:
    git_repositories = DirectoryUtils.get_git_repositories(DirectoryUtils.get_path(config["directory"]))
    git_repositories = [repo for repo in git_repositories if "outdated." not in repo]
    for repo in git_repositories:
        Git.print_git_status(DirectoryUtils.get_path(config["directory"]) + repo, config["blacklist"])

else:
    print("Operation not implemented")
