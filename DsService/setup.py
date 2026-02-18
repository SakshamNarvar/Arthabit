from setuptools import setup, find_packages

install_requires = [
    'Flask==3.1.2',
    'kafka-python==2.0.2',
    'langchain-core>=0.3.15,<0.4',
    'langchain-google-genai==2.0.5',
    'pydantic==2.10.6',
    'python-dotenv==1.0.1',
    'python-dateutil==2.9.0.post0',
    'gunicorn==25.0.3',
]

setup(
    name='ds-service',
    version='1.0',
    packages=find_packages('src'),
    package_dir={'': 'src'},
    install_requires=install_requires,
    include_package_data=True,
    python_requires='>=3.9',
    description='Data Science Service for Expense Tracker',
    author='SakshamNarvar',
)