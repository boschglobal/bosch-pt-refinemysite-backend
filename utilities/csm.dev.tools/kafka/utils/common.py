from sys import exit


class CommonUtils:
    @staticmethod
    def confirm(question='Continue (y/n)?'):
        """Get user confirmation before continue"""

        while True:
            confirmation = input(question + ' ')
            if 'n' == confirmation or 'N' == confirmation:
                exit(1)
            elif 'y' == confirmation or 'Y' == confirmation:
                break

    @staticmethod
    def environment_select():
        """Enter environment name"""

        environment = input('Enter environment name (e.g. sandbox4): ')
        return environment.strip()
    
    @staticmethod
    def color_blue_green_none_select():
        """Enter the color of the cluster to use"""
        
        while True:
            bgn = input('Select blue/green/none(without suffix) cluster (b/g/n)? ')
            if 'n' == bgn:
                blue_green_none = "none"
                blue_green_none_short = "none"
                break
            elif 'b' == bgn:
                blue_green_none = "blue"
                blue_green_none_short = "blu"
                break
            elif 'g' == bgn:
                blue_green_none = "green"
                blue_green_none_short = "gre"
                break
        return blue_green_none, blue_green_none_short
