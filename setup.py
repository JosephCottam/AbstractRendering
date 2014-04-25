from distutils.core import setup, Extension

setup(name='arpy', 
      version='0.0.1',
      description='Rendering as a binning process',
      author='Joseph Cottam',
      author_email='jcottam@indiana.edu',
      url='https://github.com/JosephCottam/AbstractRendering',
      package_dir = {'arpy' : 'python'},
      #packages = ["arpy"],    ###Might use this form if we clean up stuff from the python directory
      py_modules=['arpy.ar','arpy.categories','arpy.fast_project','arpy.geometry','arpy.infos','arpy.numeric'],
      ext_modules=[Extension('arpy.transform',
                             ['python/transform.cpp'],
                             extra_compile_args=['-std=c++11','-O3', '-Wall', '-march=native', '-fno-rtti', '-fno-exceptions', '-fPIC', '-lstdc++']),
                   Extension('arpy.transform_libdispatch',
                             ['python/transform_libdispatch.cpp'], 
                             extra_compile_args=['-std=c++11','-O3', '-Wall', '-march=native', '-fno-rtti', '-fno-exceptions', '-fPIC', '-lstdc++'])]
     )
      
