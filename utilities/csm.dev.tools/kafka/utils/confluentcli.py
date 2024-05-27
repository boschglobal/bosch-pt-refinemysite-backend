from os import path
from sys import exit

from process import ProcessUtils


class ConfluentCliUtils:
    @staticmethod
    def locate_cli():
        """Find cli"""

        output, error = ProcessUtils.run('which confluent')
        cli_path = (output.splitlines() or [None])[0]
        return cli_path

    @staticmethod
    def prompt_for_cli_path():
        """Ask user to provide path to cli"""

        while True:
            print('The confluent cli binary could not be found!')
            cli_path = input('Enter the path to the confluent cli binary: ')
            if path.isfile(cli_path):
                return cli_path

    @staticmethod
    def get_cli_dir():
        """Find the directory of the confluent cli"""

        cli_path = ConfluentCliUtils.locate_cli() or ConfluentCliUtils.prompt_for_cli_path()
        return path.join(path.dirname(cli_path), '')

    @staticmethod
    def assert_authenticated(cli_dir):
        """Check that the user is authenticated"""

        output, error = ProcessUtils.run('confluent environment list', cli_dir)
        if 'token is expired' in error or 'login' in error:
            print('Not logged in to confluent. Please run `confluent login` and retry!')
            exit(1)
        elif error:
            print(error)
            exit(1)

    @staticmethod
    def assert_environment_selected(cli_dir):
        """Check that an environment is selected"""

        output, error = ProcessUtils.run('confluent environment list', cli_dir)
        if '*' not in output:
            print('No environment selected. Run `confluent environment use ...` to select one!')
            exit(1)
        environment_list = output.splitlines()
        active_environment = [
            environment for environment in environment_list if '*' in environment][0].split(' ')
        active_environment = [environment.strip()
            for environment in active_environment if environment not in ['', '*', '|']]
        print('Using Kafka environment:',
              active_environment[0], ' (' + active_environment[1] + ')')

    @staticmethod
    def assert_cluster_selected(cli_dir):
        """Check that a cluster is selected"""

        output, error = ProcessUtils.run('confluent kafka cluster list', cli_dir)
        if '*' not in output:
            print('No cluster selected. Run `confluent kafka cluster use ...` to select one!')
            exit(1)
        cluster_list = output.splitlines()
        active_cluster = [cluster
            for cluster in cluster_list if '*' in cluster][0].split(' ')
        active_cluster = [cluster.strip()
            for cluster in active_cluster if cluster not in ['', '*', '|']]
        print('Using Kafka cluster:',
              active_cluster[0], ' (' + active_cluster[1] + ')')

    @staticmethod
    def get_service_accounts(cli_dir, environment, blue_green_short):
        """Get list of service accounts"""

        if blue_green_short == "none":
            output, error = ProcessUtils.run(
                'confluent iam service-account list | grep "csm.*{0}" | grep -vE ".*(blu|gre).*"'.format(environment), cli_dir)
        else:
            output, error = ProcessUtils.run(
                'confluent iam service-account list | grep "csm.*{0}" | grep "{1}"'.format(environment, blue_green_short), cli_dir)
        return output.splitlines()

    @staticmethod
    def get_api_key_tuples_by_service_account_ids(cli_dir, service_account_ids):
        """Get set of api key tuples for a given list of service account ids"""

        if len(service_account_ids) == 0:
            return []

        output, error = ProcessUtils.run(
            'confluent api-key list | grep -E "({0})"'.format('|'.join(service_account_ids)), cli_dir)
        api_keys_rows = output.splitlines()
        api_key_tuples = list()
        for row in api_keys_rows:
            columns = row.split('|')
            api_key_tuples.append(
                {'api-key': columns[0].strip(), 'service-account': columns[1].strip()})
        return api_key_tuples

    @staticmethod
    def get_acl_list_rows_by_service_account_ids(cli_dir, environment, service_account_ids):
        """Get ACL rows for a given list of service account ids"""

        if len(service_account_ids) == 0:
            return '', ''

        output, error = ProcessUtils.run(
            'confluent kafka acl list | grep "csm.*{0}" | grep -E "User:({1})"'.format(environment, '|'.join(service_account_ids)), cli_dir)
        return output, error

