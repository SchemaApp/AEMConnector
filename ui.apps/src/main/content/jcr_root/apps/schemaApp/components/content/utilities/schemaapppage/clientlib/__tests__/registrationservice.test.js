import {register, errorHandler} from '../registrationservice.js';


describe("register", () => {

    test('connect to Schema App', () => {
    
    const dialog = {
	    find: function(name, key) {
	    	const dataObj = {
	    		getValue : function() {
	    		return "testdata1";
	    		}
	    	};
	    	return [dataObj];
	    }
    };
    
      register(dialog);
    
      
    });
});
